package org.echoiot.server.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.MailService;
import org.echoiot.rule.engine.api.SmsService;
import org.echoiot.rule.engine.api.sms.SmsSenderFactory;
import org.echoiot.script.api.js.JsInvokeService;
import org.echoiot.script.api.tbel.TbelInvokeService;
import org.echoiot.server.actors.service.ActorService;
import org.echoiot.server.actors.tenant.DebugTbRateLimits;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.event.ErrorEvent;
import org.echoiot.server.common.data.event.LifecycleEvent;
import org.echoiot.server.common.data.event.RuleChainDebugEvent;
import org.echoiot.server.common.data.event.RuleNodeDebugEvent;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.msg.tools.TbRateLimits;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.audit.AuditLogService;
import org.echoiot.server.dao.cassandra.CassandraCluster;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.ClaimDevicesService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.event.EventService;
import org.echoiot.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.echoiot.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.rule.RuleNodeStateService;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.service.apiusage.TbApiUsageStateService;
import org.echoiot.server.service.component.ComponentDiscoveryService;
import org.echoiot.server.service.edge.rpc.EdgeRpcService;
import org.echoiot.server.service.entitiy.entityview.TbEntityViewService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.executors.ExternalCallExecutorService;
import org.echoiot.server.service.executors.SharedEventLoopGroupService;
import org.echoiot.server.service.mail.MailExecutorService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.echoiot.server.service.rpc.TbCoreDeviceRpcService;
import org.echoiot.server.service.rpc.TbRpcService;
import org.echoiot.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.echoiot.server.service.session.DeviceSessionCacheService;
import org.echoiot.server.service.sms.SmsExecutorService;
import org.echoiot.server.service.state.DeviceStateService;
import org.echoiot.server.service.telemetry.AlarmSubscriptionService;
import org.echoiot.server.service.telemetry.TelemetrySubscriptionService;
import org.echoiot.server.service.transport.TbCoreToTransportService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ActorSystemContext {

    private static final FutureCallback<Void> RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Rule Chain", th);
        }
    };
    private static final FutureCallback<Void> RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Node", th);
        }
    };

    protected final ObjectMapper mapper = new ObjectMapper();

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    @NotNull
    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Resource
    @Getter
    private TbApiUsageStateService apiUsageStateService;

    @Resource
    @Getter
    private TbApiUsageReportClient apiUsageClient;

    @Resource
    @Getter
    @Setter
    private TbServiceInfoProvider serviceInfoProvider;

    @Getter
    @Setter
    private ActorService actorService;

    @Resource
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Resource
    @Getter
    private DataDecodingEncodingService encodingService;

    @Resource
    @Getter
    private DeviceService deviceService;

    @Resource
    @Getter
    private DeviceProfileService deviceProfileService;

    @Resource
    @Getter
    private AssetProfileService assetProfileService;

    @Resource
    @Getter
    private DeviceCredentialsService deviceCredentialsService;

    @Resource
    @Getter
    private TbTenantProfileCache tenantProfileCache;

    @Resource
    @Getter
    private TbDeviceProfileCache deviceProfileCache;

    @Resource
    @Getter
    private TbAssetProfileCache assetProfileCache;

    @Resource
    @Getter
    private AssetService assetService;

    @Resource
    @Getter
    private DashboardService dashboardService;

    @Resource
    @Getter
    private TenantService tenantService;

    @Resource
    @Getter
    private TenantProfileService tenantProfileService;

    @Resource
    @Getter
    private CustomerService customerService;

    @Resource
    @Getter
    private UserService userService;

    @Resource
    @Getter
    private RuleChainService ruleChainService;

    @Resource
    @Getter
    private RuleNodeStateService ruleNodeStateService;

    @Resource
    private PartitionService partitionService;

    @Resource
    @Getter
    private TbClusterService clusterService;

    @Resource
    @Getter
    private TimeseriesService tsService;

    @Resource
    @Getter
    private AttributesService attributesService;

    @Resource
    @Getter
    private EventService eventService;

    @Resource
    @Getter
    private RelationService relationService;

    @Resource
    @Getter
    private AuditLogService auditLogService;

    @Resource
    @Getter
    private EntityViewService entityViewService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbEntityViewService tbEntityViewService;

    @Resource
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Resource
    @Getter
    private AlarmSubscriptionService alarmService;

    @Resource
    @Getter
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    @Getter
    private TbelInvokeService tbelInvokeService;

    @Resource
    @Getter
    private MailExecutorService mailExecutor;

    @Resource
    @Getter
    private SmsExecutorService smsExecutor;

    @Resource
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Resource
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Resource
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Resource
    @Getter
    private MailService mailService;

    @Resource
    @Getter
    private SmsService smsService;

    @Resource
    @Getter
    private SmsSenderFactory smsSenderFactory;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ClaimDevicesService claimDevicesService;

    @Resource
    @Getter
    private JsInvokeStats jsInvokeStats;

    //TODO: separate context for TbCore and TbRuleEngine
    @Autowired(required = false)
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired(required = false)
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Autowired(required = false)
    @Getter
    private TbCoreToTransportService tbCoreToTransportService;

    /**
     * The following Service will be null if we operate in tb-core mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    /**
     * The following Service will be null if we operate in tb-rule-engine mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeService edgeService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeRpcService edgeRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ResourceService resourceService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private OtaPackageService otaPackageService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRpcService tbRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueService queueService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetTypeService widgetTypeService;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout:10000}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.rule.chain.error_persist_frequency:3000}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency:3000}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled:true}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency:3600000}")
    @Getter
    private long statisticsPersistFrequency;

    @Value("${edges.enabled:true}")
    @Getter
    private boolean edgesEnabled;

    @Value("${cache.type:caffeine}")
    @Getter
    private String cacheType;

    @Getter
    private boolean localCacheType;

    @PostConstruct
    public void init() {
        this.localCacheType = "caffeine".equals(cacheType);
    }

    @Scheduled(fixedDelayString = "${actors.statistics.js_print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeStats.getRequests() > 0 || jsInvokeStats.getResponses() > 0 || jsInvokeStats.getFailures() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeStats.getRequests(), jsInvokeStats.getResponses(), jsInvokeStats.getFailures());
                jsInvokeStats.reset();
            }
        }
    }

    @Value("${actors.tenant.create_components_on_init:true}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service:true}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${actors.rule.allow_system_sms_service:true}")
    @Getter
    private boolean allowSystemSmsService;

    @Value("${transport.sessions.inactivity_timeout:300000}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout:3000}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled:true}")
    @Getter
    private boolean debugPerTenantEnabled;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration:50000:3600}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    @Value("${actors.rpc.sequential:false}")
    @Getter
    private boolean rpcSequential;

    @Value("${actors.rpc.max_retries:5}")
    @Getter
    private int maxRpcRetries;

    @Getter
    @Setter
    private TbActorSystem actorSystem;

    @Setter
    private TbActorRef appActor;

    @Getter
    @Setter
    private TbActorRef statsActor;

    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    public ScheduledExecutorService getScheduler() {
        return actorSystem.getScheduler();
    }

    public void persistError(TenantId tenantId, @NotNull EntityId entityId, String method, @NotNull Exception e) {
        eventService.saveAsync(ErrorEvent.builder()
                                         .tenantId(tenantId)
                                         .entityId(entityId.getId())
                                         .serviceId(getServiceId())
                                         .method(method)
                                         .error(toString(e)).build());
    }

    public void persistLifecycleEvent(TenantId tenantId, @NotNull EntityId entityId, @NotNull ComponentLifecycleEvent lcEvent, @org.jetbrains.annotations.Nullable Exception e) {
        LifecycleEvent.LifecycleEventBuilder event = LifecycleEvent.builder()
                                                                   .tenantId(tenantId)
                                                                   .entityId(entityId.getId())
                                                                   .serviceId(getServiceId())
                                                                   .lcEventType(lcEvent.name());

        if (e != null) {
            event.success(false).error(toString(e));
        } else {
            event.success(true);
        }

        eventService.saveAsync(event.build());
    }

    private String toString(@NotNull Throwable e) {
        @NotNull StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, queueName, tenantId, entityId);
    }

    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    public void persistDebugInput(TenantId tenantId, @NotNull EntityId entityId, @NotNull TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null, null);
    }

    public void persistDebugInput(TenantId tenantId, @NotNull EntityId entityId, @NotNull TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, @NotNull EntityId entityId, @NotNull TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, failureMessage);
    }

    public void persistDebugOutput(TenantId tenantId, @NotNull EntityId entityId, @NotNull TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, @NotNull EntityId entityId, @NotNull TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null, null);
    }

    private void persistDebugAsync(TenantId tenantId, @NotNull EntityId entityId, String type, @NotNull TbMsg tbMsg, String relationType, @org.jetbrains.annotations.Nullable Throwable error, @org.jetbrains.annotations.Nullable String failureMessage) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                RuleNodeDebugEvent.RuleNodeDebugEventBuilder event = RuleNodeDebugEvent.builder()
                                                                                       .tenantId(tenantId)
                                                                                       .entityId(entityId.getId())
                                                                                       .serviceId(getServiceId())
                                                                                       .eventType(type)
                                                                                       .eventEntity(tbMsg.getOriginator())
                                                                                       .msgId(tbMsg.getId())
                                                                                       .msgType(tbMsg.getType())
                                                                                       .dataType(tbMsg.getDataType().name())
                                                                                       .relationType(relationType)
                                                                                       .data(tbMsg.getData())
                                                                                       .metadata(mapper.writeValueAsString(tbMsg.getMetaData().getData()));

                if (error != null) {
                    event.error(toString(error));
                } else if (failureMessage != null) {
                    event.error(failureMessage);
                }

                ListenableFuture<Void> future = eventService.saveAsync(event.build());
                Futures.addCallback(future, RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
            } catch (IOException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, @NotNull TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            @NotNull DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, @NotNull EntityId entityId, @org.jetbrains.annotations.Nullable Throwable error) {
        RuleChainDebugEvent.RuleChainDebugEventBuilder event = RuleChainDebugEvent.builder()
                                                                                  .tenantId(tenantId)
                                                                                  .entityId(entityId.getId())
                                                                                  .serviceId(getServiceId())
                                                                                  .message("Reached debug mode rate limit!");
        if (error != null) {
            event.error(toString(error));
        }

        ListenableFuture<Void> future = eventService.saveAsync(event.build());
        Futures.addCallback(future, RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
    }

    @NotNull
    public static Exception toException(@NotNull Throwable error) {
        return error instanceof Exception ? (Exception) error : new Exception(error);
    }

    public void tell(TbActorMsg tbActorMsg) {
        appActor.tell(tbActorMsg);
    }

    public void tellWithHighPriority(TbActorMsg tbActorMsg) {
        appActor.tellWithHighPriority(tbActorMsg);
    }

    public void schedulePeriodicMsgWithDelay(@NotNull TbActorRef ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        log.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().scheduleWithFixedDelay(() -> ctx.tell(msg), delayInMs, periodInMs, TimeUnit.MILLISECONDS);
    }

    public void scheduleMsgWithDelay(@NotNull TbActorRef ctx, TbActorMsg msg, long delayInMs) {
        log.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        if (delayInMs > 0) {
            getScheduler().schedule(() -> ctx.tell(msg), delayInMs, TimeUnit.MILLISECONDS);
        } else {
            ctx.tell(msg);
        }
    }

}
