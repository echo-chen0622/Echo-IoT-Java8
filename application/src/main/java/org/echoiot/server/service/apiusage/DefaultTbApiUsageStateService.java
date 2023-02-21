package org.echoiot.server.service.apiusage;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.rule.engine.api.MailService;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.BasicTsKvEntry;
import org.echoiot.server.common.data.kv.LongDataEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.page.PageDataIterable;
import org.echoiot.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.msg.tools.SchedulerUtils;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.echoiot.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.partition.AbstractPartitionBasedService;
import org.echoiot.server.service.telemetry.InternalTelemetryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultTbApiUsageStateService extends AbstractPartitionBasedService<EntityId> implements TbApiUsageStateService {

    public static final String HOURLY = "Hourly";
    public static final FutureCallback<Integer> VOID_CALLBACK = new FutureCallback<Integer>() {
        @Override
        public void onSuccess(@Nullable Integer result) {
        }

        @Override
        public void onFailure(Throwable t) {
        }
    };
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final TenantService tenantService;
    private final TimeseriesService tsService;
    private final ApiUsageStateService apiUsageStateService;
    private final TbTenantProfileCache tenantProfileCache;
    private final MailService mailService;
    private final DbCallbackExecutorService dbExecutor;

    @Lazy
    @Resource
    private InternalTelemetryService tsWsService;

    // Entities that should be processed on this server
    final Map<EntityId, BaseApiUsageState> myUsageStates = new ConcurrentHashMap<>();
    // Entities that should be processed on other servers
    final Map<EntityId, ApiUsageState> otherUsageStates = new ConcurrentHashMap<>();

    final Set<EntityId> deletedEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;

    @Value("${usage.stats.check.cycle:60000}")
    private long nextCycleCheckInterval;

    private final Lock updateLock = new ReentrantLock();

    @NotNull
    private final ExecutorService mailExecutor;

    public DefaultTbApiUsageStateService(TbClusterService clusterService,
                                         PartitionService partitionService,
                                         TenantService tenantService,
                                         TimeseriesService tsService,
                                         ApiUsageStateService apiUsageStateService,
                                         TbTenantProfileCache tenantProfileCache,
                                         MailService mailService,
                                         DbCallbackExecutorService dbExecutor) {
        this.clusterService = clusterService;
        this.partitionService = partitionService;
        this.tenantService = tenantService;
        this.tsService = tsService;
        this.apiUsageStateService = apiUsageStateService;
        this.tenantProfileCache = tenantProfileCache;
        this.mailService = mailService;
        this.mailExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("api-usage-svc-mail"));
        this.dbExecutor = dbExecutor;
    }

    @PostConstruct
    public void init() {
        super.init();
        if (enabled) {
            log.info("Starting api usage service.");
            scheduledExecutor.scheduleAtFixedRate(this::checkStartOfNextCycle, nextCycleCheckInterval, nextCycleCheckInterval, TimeUnit.MILLISECONDS);
            log.info("Started api usage service.");
        }
    }

    @NotNull
    @Override
    protected String getServiceName() {
        return "API Usage";
    }

    @NotNull
    @Override
    protected String getSchedulerExecutorName() {
        return "api-usage-scheduled";
    }

    @Override
    public void process(@NotNull TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, @NotNull TbCallback callback) {
        ToUsageStatsServiceMsg statsMsg = msg.getValue();

        @NotNull TenantId tenantId = TenantId.fromUUID(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
        EntityId entityId;
        if (statsMsg.getCustomerIdMSB() != 0 && statsMsg.getCustomerIdLSB() != 0) {
            entityId = new CustomerId(new UUID(statsMsg.getCustomerIdMSB(), statsMsg.getCustomerIdLSB()));
        } else {
            entityId = tenantId;
        }

        processEntityUsageStats(tenantId, entityId, statsMsg.getValuesList());
        callback.onSuccess();
    }

    private void processEntityUsageStats(TenantId tenantId, EntityId entityId, @NotNull List<UsageStatsKVProto> values) {
        if (deletedEntities.contains(entityId)) return;

        BaseApiUsageState usageState;
        List<TsKvEntry> updatedEntries;
        Map<ApiFeature, ApiUsageStateValue> result;

        updateLock.lock();
        try {
            usageState = getOrFetchState(tenantId, entityId);
            long ts = usageState.getCurrentCycleTs();
            long hourTs = usageState.getCurrentHourTs();
            long newHourTs = SchedulerUtils.getStartOfCurrentHour();
            if (newHourTs != hourTs) {
                usageState.setHour(newHourTs);
            }
            updatedEntries = new ArrayList<>(ApiUsageRecordKey.values().length);
            @NotNull Set<ApiFeature> apiFeatures = new HashSet<>();
            for (@NotNull UsageStatsKVProto kvProto : values) {
                @NotNull ApiUsageRecordKey recordKey = ApiUsageRecordKey.valueOf(kvProto.getKey());
                long newValue = usageState.add(recordKey, kvProto.getValue());
                updatedEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(recordKey.getApiCountKey(), newValue)));
                long newHourlyValue = usageState.addToHourly(recordKey, kvProto.getValue());
                updatedEntries.add(new BasicTsKvEntry(newHourTs, new LongDataEntry(recordKey.getApiCountKey() + HOURLY, newHourlyValue)));
                apiFeatures.add(recordKey.getApiFeature());
            }
            if (usageState.getEntityType() == EntityType.TENANT && !usageState.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
                result = ((TenantApiUsageState) usageState).checkStateUpdatedDueToThreshold(apiFeatures);
            } else {
                result = Collections.emptyMap();
            }
        } finally {
            updateLock.unlock();
        }
        tsWsService.saveAndNotifyInternal(tenantId, usageState.getApiUsageState().getId(), updatedEntries, VOID_CALLBACK);
        if (!result.isEmpty()) {
            persistAndNotify(usageState, result);
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public ApiUsageState getApiUsageState(TenantId tenantId) {
        TenantApiUsageState tenantState = (TenantApiUsageState) myUsageStates.get(tenantId);
        if (tenantState != null) {
            return tenantState.getApiUsageState();
        } else {
            ApiUsageState state = otherUsageStates.get(tenantId);
            if (state != null) {
                return state;
            } else {
                if (partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
                    return getOrFetchState(tenantId, tenantId).getApiUsageState();
                } else {
                    state = otherUsageStates.get(tenantId);
                    if (state == null) {
                        state = apiUsageStateService.findTenantApiUsageState(tenantId);
                        if (state != null) {
                            otherUsageStates.put(tenantId, state);
                        }
                    }
                    return state;
                }
            }
        }
    }

    @Override
    public void onApiUsageStateUpdate(TenantId tenantId) {
        otherUsageStates.remove(tenantId);
    }

    @Override
    public void onTenantProfileUpdate(TenantProfileId tenantProfileId) {
        log.info("[{}] On Tenant Profile Update", tenantProfileId);
        @org.jetbrains.annotations.Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantProfileId);
        updateLock.lock();
        try {
            myUsageStates.values().stream()
                    .filter(state -> state.getEntityType() == EntityType.TENANT)
                    .map(state -> (TenantApiUsageState) state)
                    .forEach(state -> {
                        if (tenantProfile.getId().equals(state.getTenantProfileId())) {
                            updateTenantState(state, tenantProfile);
                        }
                    });
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void onTenantUpdate(TenantId tenantId) {
        log.info("[{}] On Tenant Update.", tenantId);
        @org.jetbrains.annotations.Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        updateLock.lock();
        try {
            TenantApiUsageState state = (TenantApiUsageState) myUsageStates.get(tenantId);
            if (state != null && !state.getTenantProfileId().equals(tenantProfile.getId())) {
                updateTenantState(state, tenantProfile);
            }
        } finally {
            updateLock.unlock();
        }
    }

    private void updateTenantState(@NotNull TenantApiUsageState state, @NotNull TenantProfile profile) {
        TenantProfileData oldProfileData = state.getTenantProfileData();
        state.setTenantProfileId(profile.getId());
        state.setTenantProfileData(profile.getProfileData());
        Map<ApiFeature, ApiUsageStateValue> result = state.checkStateUpdatedDueToThresholds();
        if (!result.isEmpty()) {
            persistAndNotify(state, result);
        }
        updateProfileThresholds(state.getTenantId(), state.getApiUsageState().getId(),
                oldProfileData.getConfiguration(), profile.getProfileData().getConfiguration());
    }

    private void addEntityState(@NotNull TopicPartitionInfo tpi, @NotNull BaseApiUsageState state) {
        EntityId entityId = state.getEntityId();
        Set<EntityId> entityIds = partitionedEntities.get(tpi);
        if (entityIds != null) {
            entityIds.add(entityId);
            myUsageStates.put(entityId, state);
        } else {
            log.debug("[{}] belongs to external partition {}", entityId, tpi.getFullTopicName());
            throw new RuntimeException(entityId.getEntityType() + " belongs to external partition " + tpi.getFullTopicName() + "!");
        }
    }

    private void updateProfileThresholds(TenantId tenantId, ApiUsageStateId id,
                                         @org.jetbrains.annotations.Nullable TenantProfileConfiguration oldData, @NotNull TenantProfileConfiguration newData) {
        long ts = System.currentTimeMillis();
        @NotNull List<TsKvEntry> profileThresholds = new ArrayList<>();
        for (@NotNull ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            long newProfileThreshold = newData.getProfileThreshold(key);
            if (oldData == null || oldData.getProfileThreshold(key) != newProfileThreshold) {
                log.info("[{}] Updating profile threshold [{}]:[{}]", tenantId, key, newProfileThreshold);
                profileThresholds.add(new BasicTsKvEntry(ts, new LongDataEntry(key.getApiLimitKey(), newProfileThreshold)));
            }
        }
        if (!profileThresholds.isEmpty()) {
            tsWsService.saveAndNotifyInternal(tenantId, id, profileThresholds, VOID_CALLBACK);
        }
    }

    public void onTenantDelete(TenantId tenantId) {
        deletedEntities.add(tenantId);
        myUsageStates.remove(tenantId);
        otherUsageStates.remove(tenantId);
    }

    @Override
    public void onCustomerDelete(CustomerId customerId) {
        deletedEntities.add(customerId);
        myUsageStates.remove(customerId);
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(EntityId entityId) {
        myUsageStates.remove(entityId);
    }

    private void persistAndNotify(@NotNull BaseApiUsageState state, @NotNull Map<ApiFeature, ApiUsageStateValue> result) {
        log.info("[{}] Detected update of the API state for {}: {}", state.getEntityId(), state.getEntityType(), result);
        apiUsageStateService.update(state.getApiUsageState());
        clusterService.onApiStateChange(state.getApiUsageState(), null);
        long ts = System.currentTimeMillis();
        @NotNull List<TsKvEntry> stateTelemetry = new ArrayList<>();
        result.forEach((apiFeature, aState) -> stateTelemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(apiFeature.getApiStateKey(), aState.name()))));
        tsWsService.saveAndNotifyInternal(state.getTenantId(), state.getApiUsageState().getId(), stateTelemetry, VOID_CALLBACK);

        if (state.getEntityType() == EntityType.TENANT && !state.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
            String email = tenantService.findTenantById(state.getTenantId()).getEmail();
            if (StringUtils.isNotEmpty(email)) {
                result.forEach((apiFeature, stateValue) -> {
                    mailExecutor.submit(() -> {
                        try {
                            mailService.sendApiFeatureStateEmail(apiFeature, stateValue, email, createStateMailMessage((TenantApiUsageState) state, apiFeature, stateValue));
                        } catch (EchoiotException e) {
                            log.warn("[{}] Can't send update of the API state to tenant with provided email [{}]", state.getTenantId(), email, e);
                        }
                    });
                });
            } else {
                log.warn("[{}] Can't send update of the API state to tenant with empty email!", state.getTenantId());
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    private ApiUsageStateMailMessage createStateMailMessage(@NotNull TenantApiUsageState state, @NotNull ApiFeature apiFeature, ApiUsageStateValue stateValue) {
        @NotNull StateChecker checker = getStateChecker(stateValue);
        for (ApiUsageRecordKey apiUsageRecordKey : ApiUsageRecordKey.getKeys(apiFeature)) {
            long threshold = state.getProfileThreshold(apiUsageRecordKey);
            long warnThreshold = state.getProfileWarnThreshold(apiUsageRecordKey);
            long value = state.get(apiUsageRecordKey);
            if (checker.check(threshold, warnThreshold, value)) {
                return new ApiUsageStateMailMessage(apiUsageRecordKey, threshold, value);
            }
        }
        return null;
    }

    @NotNull
    private StateChecker getStateChecker(ApiUsageStateValue stateValue) {
        if (ApiUsageStateValue.ENABLED.equals(stateValue)) {
            return (t, wt, v) -> true;
        } else if (ApiUsageStateValue.WARNING.equals(stateValue)) {
            return (t, wt, v) -> v < t && v >= wt;
        } else {
            return (t, wt, v) -> v >= t;
        }
    }

    @Override
    public ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id) {
        return apiUsageStateService.findApiUsageStateById(tenantId, id);
    }

    private interface StateChecker {
        boolean check(long threshold, long warnThreshold, long value);
    }

    private void checkStartOfNextCycle() {
        updateLock.lock();
        try {
            long now = System.currentTimeMillis();
            myUsageStates.values().forEach(state -> {
                if ((state.getNextCycleTs() < now) && (now - state.getNextCycleTs() < TimeUnit.HOURS.toMillis(1))) {
                    state.setCycles(state.getNextCycleTs(), SchedulerUtils.getStartOfNextNextMonth());
                    saveNewCounts(state, Arrays.asList(ApiUsageRecordKey.values()));
                    if (state.getEntityType() == EntityType.TENANT && !state.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
                        TenantId tenantId = state.getTenantId();
                        updateTenantState((TenantApiUsageState) state, tenantProfileCache.get(tenantId));
                    }
                }
            });
        } finally {
            updateLock.unlock();
        }
    }

    private void saveNewCounts(@NotNull BaseApiUsageState state, @NotNull List<ApiUsageRecordKey> keys) {
        @NotNull List<TsKvEntry> counts = keys.stream()
                                              .map(key -> new BasicTsKvEntry(state.getCurrentCycleTs(), new LongDataEntry(key.getApiCountKey(), 0L)))
                                              .collect(Collectors.toList());

        tsWsService.saveAndNotifyInternal(state.getTenantId(), state.getApiUsageState().getId(), counts, VOID_CALLBACK);
    }

    @NotNull
    BaseApiUsageState getOrFetchState(TenantId tenantId, @org.jetbrains.annotations.Nullable EntityId entityId) {
        if (entityId == null || entityId.isNullUid()) {
            entityId = tenantId;
        }
        BaseApiUsageState state = myUsageStates.get(entityId);
        if (state != null) {
            return state;
        }

        ApiUsageState storedState = apiUsageStateService.findApiUsageStateByEntityId(entityId);
        if (storedState == null) {
            try {
                storedState = apiUsageStateService.createDefaultApiUsageState(tenantId, entityId);
            } catch (Exception e) {
                storedState = apiUsageStateService.findApiUsageStateByEntityId(entityId);
            }
        }
        if (entityId.getEntityType() == EntityType.TENANT) {
            if (!entityId.equals(TenantId.SYS_TENANT_ID)) {
                state = new TenantApiUsageState(tenantProfileCache.get((TenantId) entityId), storedState);
            } else {
                state = new TenantApiUsageState(storedState);
            }
        } else {
            state = new CustomerApiUsageState(storedState);
        }

        @NotNull List<ApiUsageRecordKey> newCounts = new ArrayList<>();
        try {
            List<TsKvEntry> dbValues = tsService.findAllLatest(tenantId, storedState.getId()).get();
            for (@NotNull ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                boolean cycleEntryFound = false;
                boolean hourlyEntryFound = false;
                for (@NotNull TsKvEntry tsKvEntry : dbValues) {
                    if (tsKvEntry.getKey().equals(key.getApiCountKey())) {
                        cycleEntryFound = true;

                        boolean oldCount = tsKvEntry.getTs() == state.getCurrentCycleTs();
                        state.put(key, oldCount ? tsKvEntry.getLongValue().get() : 0L);

                        if (!oldCount) {
                            newCounts.add(key);
                        }
                    } else if (tsKvEntry.getKey().equals(key.getApiCountKey() + HOURLY)) {
                        hourlyEntryFound = true;
                        state.putHourly(key, tsKvEntry.getTs() == state.getCurrentHourTs() ? tsKvEntry.getLongValue().get() : 0L);
                    }
                    if (cycleEntryFound && hourlyEntryFound) {
                        break;
                    }
                }
            }
            log.debug("[{}] Initialized state: {}", entityId, storedState);
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
            if (tpi.isMyPartition()) {
                addEntityState(tpi, state);
            } else {
                otherUsageStates.put(entityId, state.getApiUsageState());
            }
            saveNewCounts(state, newCounts);
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch api usage state from db.", tenantId, e);
        }

        return state;
    }

    @Override
    protected void onRepartitionEvent() {
        otherUsageStates.entrySet().removeIf(entry ->
                partitionService.resolve(ServiceType.TB_CORE, entry.getValue().getTenantId(), entry.getKey()).isMyPartition());
    }

    @NotNull
    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(@NotNull Set<TopicPartitionInfo> addedPartitions) {
        @NotNull var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
        try {
            log.info("Initializing tenant states.");
            updateLock.lock();
            try {
                @NotNull PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, 1024);
                for (@NotNull Tenant tenant : tenantIterator) {
                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), tenant.getId());
                    if (addedPartitions.contains(tpi)) {
                        if (!myUsageStates.containsKey(tenant.getId()) && tpi.isMyPartition()) {
                            log.debug("[{}] Initializing tenant state.", tenant.getId());
                            result.computeIfAbsent(tpi, tmp -> new ArrayList<>()).add(dbExecutor.submit(() -> {
                                try {
                                    updateTenantState((TenantApiUsageState) getOrFetchState(tenant.getId(), tenant.getId()), tenantProfileCache.get(tenant.getTenantProfileId()));
                                    log.debug("[{}] Initialized tenant state.", tenant.getId());
                                } catch (Exception e) {
                                    log.warn("[{}] Failed to initialize tenant API state", tenant.getId(), e);
                                }
                                return null;
                            }));
                        }
                    } else {
                        log.debug("[{}][{}] Tenant doesn't belong to current partition. tpi [{}]", tenant.getName(), tenant.getId(), tpi);
                    }
                }
            } finally {
                updateLock.unlock();
            }
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
        return result;
    }

    @PreDestroy
    private void destroy() {
        super.stop();
        if (mailExecutor != null) {
            mailExecutor.shutdownNow();
        }
    }
}
