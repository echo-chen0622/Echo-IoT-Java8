package org.echoiot.server.dao.alarm;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.alarm.*;
import org.echoiot.server.common.data.exception.ApiUsageLimitsExceededException;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.common.data.query.AlarmDataQuery;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntityRelationsQuery;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationsSearchParameters;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmService extends AbstractEntityService implements AlarmService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Resource
    private AlarmDao alarmDao;

    @Resource
    private EntityService entityService;

    @Resource
    private DataValidator<Alarm> alarmDataValidator;

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool(EchoiotThreadFactory.forName("alarm-service"));
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(@NotNull Alarm alarm) {
        return createOrUpdateAlarm(alarm, true);
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(@NotNull Alarm alarm, boolean alarmCreationEnabled) {
        alarmDataValidator.validate(alarm, Alarm::getTenantId);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            alarm.setCustomerId(entityService.fetchEntityCustomerId(alarm.getTenantId(), alarm.getOriginator()));
            if (alarm.getId() == null) {
                @org.jetbrains.annotations.Nullable Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType());
                if (existing == null || existing.getStatus().isCleared()) {
                    if (!alarmCreationEnabled) {
                        throw new ApiUsageLimitsExceededException("Alarms creation is disabled");
                    }
                    return createAlarm(alarm);
                } else {
                    return updateAlarm(existing, alarm);
                }
            } else {
                return updateAlarm(alarm);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestByOriginatorAndTypeAsync(tenantId, originator, type);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                               @NotNull AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateEntityDataPageLink(query.getPageLink());
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    @NotNull
    @Override
    @Transactional
    public AlarmOperationResult deleteAlarm(TenantId tenantId, @NotNull AlarmId alarmId) {
        log.debug("Deleting Alarm Id: {}", alarmId);
        Alarm alarm = alarmDao.findAlarmById(tenantId, alarmId.getId());
        if (alarm == null) {
            return new AlarmOperationResult(alarm, false);
        }
        @NotNull AlarmOperationResult result = new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
        deleteEntityRelations(tenantId, alarm.getId());
        alarmDao.removeById(tenantId, alarm.getUuidId());
        return result;
    }

    @NotNull
    private AlarmOperationResult createAlarm(@NotNull Alarm alarm) throws InterruptedException, ExecutionException {
        log.debug("New Alarm : {}", alarm);
        Alarm saved = alarmDao.save(alarm.getTenantId(), alarm);
        @NotNull List<EntityId> propagatedEntitiesList = createEntityAlarmRecords(saved);
        return new AlarmOperationResult(saved, true, true, propagatedEntitiesList);
    }

    @NotNull
    private List<EntityId> createEntityAlarmRecords(@NotNull Alarm alarm) throws InterruptedException, ExecutionException {
        @NotNull Set<EntityId> propagatedEntitiesSet = new LinkedHashSet<>();
        propagatedEntitiesSet.add(alarm.getOriginator());
        if (alarm.isPropagate()) {
            propagatedEntitiesSet.addAll(getRelatedEntities(alarm));
        }
        if (alarm.isPropagateToOwner()) {
            propagatedEntitiesSet.add(alarm.getCustomerId() != null ? alarm.getCustomerId() : alarm.getTenantId());
        }
        if (alarm.isPropagateToTenant()) {
            propagatedEntitiesSet.add(alarm.getTenantId());
        }
        for (EntityId entityId : propagatedEntitiesSet) {
            createEntityAlarmRecord(alarm.getTenantId(), entityId, alarm);
        }
        return new ArrayList<>(propagatedEntitiesSet);
    }

    @NotNull
    private Set<EntityId> getRelatedEntities(@NotNull Alarm alarm) throws InterruptedException, ExecutionException {
        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        @NotNull RelationsSearchParameters parameters = new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, false);
        query.setParameters(parameters);
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> relations = relationService.findByQuery(alarm.getTenantId(), query).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            relations = relations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        return relations.map(EntityRelation::getFrom).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private AlarmOperationResult updateAlarm(@NotNull Alarm update) {
        alarmDataValidator.validate(update, Alarm::getTenantId);
        return getAndUpdate(update.getTenantId(), update.getId(),
                (alarm) -> alarm == null ? null : updateAlarm(alarm, update));
    }

    @NotNull
    private AlarmOperationResult updateAlarm(@NotNull Alarm oldAlarm, @NotNull Alarm newAlarm) {
        boolean propagationEnabled = !oldAlarm.isPropagate() && newAlarm.isPropagate();
        boolean propagationToOwnerEnabled = !oldAlarm.isPropagateToOwner() && newAlarm.isPropagateToOwner();
        boolean propagationToTenantEnabled = !oldAlarm.isPropagateToTenant() && newAlarm.isPropagateToTenant();
        Alarm result = alarmDao.save(newAlarm.getTenantId(), merge(oldAlarm, newAlarm));
        List<EntityId> propagatedEntitiesList;
        if (propagationEnabled || propagationToOwnerEnabled || propagationToTenantEnabled) {
            try {
                propagatedEntitiesList = createEntityAlarmRecords(result);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update alarm relations [{}]", result, e);
                throw new RuntimeException(e);
            }
        } else {
            propagatedEntitiesList = new ArrayList<>(getPropagationEntityIds(result));
        }
        return new AlarmOperationResult(result, true, propagatedEntitiesList);
    }

    @NotNull
    @Override
    public ListenableFuture<AlarmOperationResult> ackAlarm(TenantId tenantId, @NotNull AlarmId alarmId, long ackTime) {
        return getAndUpdateAsync(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isAck()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    @NotNull AlarmStatus oldStatus = alarm.getStatus();
                    @NotNull AlarmStatus newStatus = oldStatus.isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK;
                    alarm.setStatus(newStatus);
                    alarm.setAckTs(ackTime);
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @NotNull
    @Override
    public ListenableFuture<AlarmOperationResult> clearAlarm(TenantId tenantId, @NotNull AlarmId alarmId, @org.jetbrains.annotations.Nullable JsonNode details, long clearTime) {
        return getAndUpdateAsync(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isCleared()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    @NotNull AlarmStatus oldStatus = alarm.getStatus();
                    @NotNull AlarmStatus newStatus = oldStatus.isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
                    alarm.setStatus(newStatus);
                    alarm.setClearTs(clearTime);
                    if (details != null) {
                        alarm.setDetails(details);
                    }
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, @NotNull AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmById(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, @NotNull AlarmId alarmId) {
        log.trace("Executing findAlarmByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @NotNull
    @Override
    public ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, @NotNull AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return Futures.transformAsync(alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId()),
                a -> {
                    @NotNull AlarmInfo alarmInfo = new AlarmInfo(a);
                    return Futures.transform(
                            entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                                alarmInfo.setOriginatorName(originatorName);
                                return alarmInfo;
                            }, MoreExecutors.directExecutor());
                }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, @NotNull AlarmQuery query) {
        PageData<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            return fetchAlarmsOriginators(tenantId, alarms);
        }
        return Futures.immediateFuture(alarms);
    }

    @NotNull
    @Override
    public ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, @NotNull AlarmQuery query) {
        PageData<AlarmInfo> alarms = alarmDao.findCustomerAlarms(tenantId, customerId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            return fetchAlarmsOriginators(tenantId, alarms);
        }
        return Futures.immediateFuture(alarms);
    }

    @NotNull
    private ListenableFuture<PageData<AlarmInfo>> fetchAlarmsOriginators(TenantId tenantId, @NotNull PageData<AlarmInfo> alarms) {
        @NotNull List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(alarms.getData().size());
        for (@NotNull AlarmInfo alarmInfo : alarms.getData()) {
            alarmFutures.add(Futures.transform(
                    entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                        if (originatorName == null) {
                            originatorName = "Deleted";
                        }
                        alarmInfo.setOriginatorName(originatorName);
                        return alarmInfo;
                    }, MoreExecutors.directExecutor()
            ));
        }
        return Futures.transform(Futures.successfulAsList(alarmFutures),
                alarmInfos -> new PageData<>(alarmInfos, alarms.getTotalPages(), alarms.getTotalElements(),
                        alarms.hasNext()), MoreExecutors.directExecutor());
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, @org.jetbrains.annotations.Nullable AlarmSearchStatus alarmSearchStatus,
                                                  @org.jetbrains.annotations.Nullable AlarmStatus alarmStatus) {
        @org.jetbrains.annotations.Nullable Set<AlarmStatus> statusList = null;
        if (alarmSearchStatus != null) {
            statusList = alarmSearchStatus.getStatuses();
        } else if (alarmStatus != null) {
            statusList = Collections.singleton(alarmStatus);
        }

        Set<AlarmSeverity> alarmSeverities = alarmDao.findAlarmSeverities(tenantId, entityId, statusList);

        return alarmSeverities.stream().min(AlarmSeverity::compareTo).orElse(null);
    }

    @Override
    public void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId) {
        alarmDao.deleteEntityAlarmRecords(tenantId, entityId);
    }

    @NotNull
    private Alarm merge(@NotNull Alarm existing, @NotNull Alarm alarm) {
        if (alarm.getStartTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getStartTs());
        }
        if (alarm.getEndTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getEndTs());
        }
        if (alarm.getClearTs() > existing.getClearTs()) {
            existing.setClearTs(alarm.getClearTs());
        }
        if (alarm.getAckTs() > existing.getAckTs()) {
            existing.setAckTs(alarm.getAckTs());
        }
        existing.setStatus(alarm.getStatus());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setCustomerId(alarm.getCustomerId());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
        existing.setPropagateToOwner(existing.isPropagateToOwner() || alarm.isPropagateToOwner());
        existing.setPropagateToTenant(existing.isPropagateToTenant() || alarm.isPropagateToTenant());
        List<String> existingPropagateRelationTypes = existing.getPropagateRelationTypes();
        List<String> newRelationTypes = alarm.getPropagateRelationTypes();
        if (!CollectionUtils.isEmpty(newRelationTypes)) {
            if (!CollectionUtils.isEmpty(existingPropagateRelationTypes)) {
                existing.setPropagateRelationTypes(Stream.concat(existingPropagateRelationTypes.stream(), newRelationTypes.stream())
                        .distinct()
                        .collect(Collectors.toList()));
            } else {
                existing.setPropagateRelationTypes(newRelationTypes);
            }
        }
        return existing;
    }

    @NotNull
    private Set<EntityId> getPropagationEntityIds(@NotNull Alarm alarm) {
        if (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToTenant()) {
            List<EntityAlarm> entityAlarms = alarmDao.findEntityAlarmRecords(alarm.getTenantId(), alarm.getId());
            return entityAlarms.stream().map(EntityAlarm::getEntityId).collect(Collectors.toSet());
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private void createEntityAlarmRecord(TenantId tenantId, EntityId entityId, @NotNull Alarm alarm) {
        @NotNull EntityAlarm entityAlarm = new EntityAlarm(tenantId, entityId, alarm.getCreatedTime(), alarm.getType(), alarm.getCustomerId(), alarm.getId());
        try {
            alarmDao.createEntityAlarmRecord(entityAlarm);
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity alarm record: {}", tenantId, entityAlarm, e);
        }
    }

    @NotNull
    private <T> ListenableFuture<T> getAndUpdateAsync(TenantId tenantId, @NotNull AlarmId alarmId, @NotNull Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        ListenableFuture<Alarm> entity = alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
        return Futures.transform(entity, function, readResultsProcessingExecutor);
    }

    private <T> T getAndUpdate(TenantId tenantId, @NotNull AlarmId alarmId, @NotNull Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        Alarm entity = alarmDao.findAlarmById(tenantId, alarmId.getId());
        return function.apply(entity);
    }
}
