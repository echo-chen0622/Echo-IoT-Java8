package org.echoiot.server.service.queue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rpc.RpcError;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.echoiot.server.queue.provider.TbCoreQueueFactory;
import org.echoiot.server.queue.util.AfterStartUp;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.apiusage.TbApiUsageStateService;
import org.echoiot.server.service.edge.EdgeNotificationService;
import org.echoiot.server.service.ota.OtaPackageStateService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.echoiot.server.service.queue.processing.AbstractConsumerService;
import org.echoiot.server.service.queue.processing.IdMsgPair;
import org.echoiot.server.service.rpc.TbCoreDeviceRpcService;
import org.echoiot.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.echoiot.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.echoiot.server.service.state.DeviceStateService;
import org.echoiot.server.service.subscription.SubscriptionManagerService;
import org.echoiot.server.service.subscription.TbLocalSubscriptionService;
import org.echoiot.server.service.subscription.TbSubscriptionUtils;
import org.echoiot.server.service.sync.vc.GitVersionControlQueueService;
import org.echoiot.server.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultTbCoreConsumerService extends AbstractConsumerService<ToCoreNotificationMsg> implements TbCoreConsumerService {

    @Value("${queue.core.poll-interval}")
    private long pollDuration;
    @Value("${queue.core.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.core.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${queue.core.ota.pack-interval-ms:60000}")
    private long firmwarePackInterval;
    @Value("${queue.core.ota.pack-size:100}")
    private int firmwarePackSize;

    private final TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> mainConsumer;
    private final DeviceStateService stateService;
    private final TbApiUsageStateService statsService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final SubscriptionManagerService subscriptionManagerService;
    private final TbCoreDeviceRpcService tbCoreDeviceRpcService;
    private final EdgeNotificationService edgeNotificationService;
    private final OtaPackageStateService firmwareStateService;
    private final GitVersionControlQueueService vcQueueService;
    @NotNull
    private final TbCoreConsumerStats stats;
    protected final TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> usageStatsConsumer;
    private final TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> firmwareStatesConsumer;

    protected volatile ExecutorService usageStatsExecutor;

    private volatile ExecutorService firmwareStatesExecutor;

    public DefaultTbCoreConsumerService(@NotNull TbCoreQueueFactory tbCoreQueueFactory,
                                        ActorSystemContext actorContext,
                                        DeviceStateService stateService,
                                        TbLocalSubscriptionService localSubscriptionService,
                                        SubscriptionManagerService subscriptionManagerService,
                                        DataDecodingEncodingService encodingService,
                                        TbCoreDeviceRpcService tbCoreDeviceRpcService,
                                        @NotNull StatsFactory statsFactory,
                                        TbDeviceProfileCache deviceProfileCache,
                                        TbAssetProfileCache assetProfileCache,
                                        TbApiUsageStateService statsService,
                                        TbTenantProfileCache tenantProfileCache,
                                        TbApiUsageStateService apiUsageStateService,
                                        EdgeNotificationService edgeNotificationService,
                                        OtaPackageStateService firmwareStateService,
                                        GitVersionControlQueueService vcQueueService,
                                        PartitionService partitionService,
                                        Optional<JwtSettingsService> jwtSettingsService) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService, tbCoreQueueFactory.createToCoreNotificationsMsgConsumer(), jwtSettingsService);
        this.mainConsumer = tbCoreQueueFactory.createToCoreMsgConsumer();
        this.usageStatsConsumer = tbCoreQueueFactory.createToUsageStatsServiceMsgConsumer();
        this.firmwareStatesConsumer = tbCoreQueueFactory.createToOtaPackageStateServiceMsgConsumer();
        this.stateService = stateService;
        this.localSubscriptionService = localSubscriptionService;
        this.subscriptionManagerService = subscriptionManagerService;
        this.tbCoreDeviceRpcService = tbCoreDeviceRpcService;
        this.edgeNotificationService = edgeNotificationService;
        this.stats = new TbCoreConsumerStats(statsFactory);
        this.statsService = statsService;
        this.firmwareStateService = firmwareStateService;
        this.vcQueueService = vcQueueService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-core-consumer", "tb-core-notifications-consumer");
        this.usageStatsExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("tb-core-usage-stats-consumer"));
        this.firmwareStatesExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("tb-core-firmware-notifications-consumer"));
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
        if (usageStatsExecutor != null) {
            usageStatsExecutor.shutdownNow();
        }
        if (firmwareStatesExecutor != null) {
            firmwareStatesExecutor.shutdownNow();
        }
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        super.onApplicationEvent(event);
        launchUsageStatsConsumer();
        launchOtaPackageUpdateNotificationConsumer();
    }

    @Override
    protected void onTbApplicationEvent(@NotNull PartitionChangeEvent event) {
        if (event.getServiceType().equals(getServiceType())) {
            log.info("Subscribing to partitions: {}", event.getPartitions());
            this.mainConsumer.subscribe(event.getPartitions());
            this.usageStatsConsumer.subscribe(
                    event
                            .getPartitions()
                            .stream()
                            .map(tpi -> tpi.newByTopic(usageStatsConsumer.getTopic()))
                            .collect(Collectors.toSet()));
        }
        this.firmwareStatesConsumer.subscribe();
    }

    @Override
    protected void launchMainConsumers() {
        consumersExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToCoreMsg>> msgs = mainConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    @NotNull List<IdMsgPair<ToCoreMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    @NotNull ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    @NotNull CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    @NotNull TbPackProcessingContext<TbProtoQueueMsg<ToCoreMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    @NotNull PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
                    @NotNull Future<?> packSubmitFuture = consumersExecutor.submit(() -> {
                        orderedMsgList.forEach((element) -> {
                            UUID id = element.getUuid();
                            TbProtoQueueMsg<ToCoreMsg> msg = element.getMsg();
                            log.trace("[{}] Creating main callback for message: {}", id, msg.getValue());
                            @NotNull TbCallback callback = new TbPackCallback<>(id, ctx);
                            try {
                                ToCoreMsg toCoreMsg = msg.getValue();
                                pendingMsgHolder.setToCoreMsg(toCoreMsg);
                                if (toCoreMsg.hasToSubscriptionMgrMsg()) {
                                    log.trace("[{}] Forwarding message to subscription manager service {}", id, toCoreMsg.getToSubscriptionMgrMsg());
                                    forwardToSubMgrService(toCoreMsg.getToSubscriptionMgrMsg(), callback);
                                } else if (toCoreMsg.hasToDeviceActorMsg()) {
                                    log.trace("[{}] Forwarding message to device actor {}", id, toCoreMsg.getToDeviceActorMsg());
                                    forwardToDeviceActor(toCoreMsg.getToDeviceActorMsg(), callback);
                                } else if (toCoreMsg.hasDeviceStateServiceMsg()) {
                                    log.trace("[{}] Forwarding message to state service {}", id, toCoreMsg.getDeviceStateServiceMsg());
                                    forwardToStateService(toCoreMsg.getDeviceStateServiceMsg(), callback);
                                } else if (toCoreMsg.hasEdgeNotificationMsg()) {
                                    log.trace("[{}] Forwarding message to edge service {}", id, toCoreMsg.getEdgeNotificationMsg());
                                    forwardToEdgeNotificationService(toCoreMsg.getEdgeNotificationMsg(), callback);
                                } else if (toCoreMsg.hasDeviceActivityMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceActivityMsg());
                                    forwardToStateService(toCoreMsg.getDeviceActivityMsg(), callback);
                                } else if (!toCoreMsg.getToDeviceActorNotificationMsg().isEmpty()) {
                                    Optional<TbActorMsg> actorMsg = encodingService.decode(toCoreMsg.getToDeviceActorNotificationMsg().toByteArray());
                                    if (actorMsg.isPresent()) {
                                        @NotNull TbActorMsg tbActorMsg = actorMsg.get();
                                        if (tbActorMsg.getMsgType().equals(MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG)) {
                                            tbCoreDeviceRpcService.forwardRpcRequestToDeviceActor((ToDeviceRpcRequestActorMsg) tbActorMsg);
                                        } else {
                                            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                                            actorContext.tell(actorMsg.get());
                                        }
                                    }
                                    callback.onSuccess();
                                }
                            } catch (Throwable e) {
                                log.warn("[{}] Failed to process message: {}", id, msg, e);
                                callback.onFailure(e);
                            }
                        });
                    });
                    if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                        if (!packSubmitFuture.isDone()) {
                            packSubmitFuture.cancel(true);
                            ToCoreMsg lastSubmitMsg = pendingMsgHolder.getToCoreMsg();
                            log.info("Timeout to process message: {}", lastSubmitMsg);
                        }
                        ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
                    }
                    mainConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
            log.info("TB Core Consumer stopped.");
        });
    }

    private static class PendingMsgHolder {
        @Getter
        @Setter
        private volatile ToCoreMsg toCoreMsg;
    }

    @NotNull
    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected void handleNotification(UUID id, @NotNull TbProtoQueueMsg<ToCoreNotificationMsg> msg, @NotNull TbCallback callback) {
        ToCoreNotificationMsg toCoreNotification = msg.getValue();
        if (toCoreNotification.hasToLocalSubscriptionServiceMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getToLocalSubscriptionServiceMsg());
            forwardToLocalSubMgrService(toCoreNotification.getToLocalSubscriptionServiceMsg(), callback);
        } else if (toCoreNotification.hasFromDeviceRpcResponse()) {
            log.trace("[{}] Forwarding message to RPC service {}", id, toCoreNotification.getFromDeviceRpcResponse());
            forwardToCoreRpcService(toCoreNotification.getFromDeviceRpcResponse(), callback);
        } else if (toCoreNotification.getComponentLifecycleMsg() != null && !toCoreNotification.getComponentLifecycleMsg().isEmpty()) {
            handleComponentLifecycleMsg(id, toCoreNotification.getComponentLifecycleMsg());
            callback.onSuccess();
        } else if (!toCoreNotification.getEdgeEventUpdateMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getEdgeEventUpdateMsg().toByteArray()), callback);
        } else if (!toCoreNotification.getToEdgeSyncRequestMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getToEdgeSyncRequestMsg().toByteArray()), callback);
        } else if (!toCoreNotification.getFromEdgeSyncResponseMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getFromEdgeSyncResponseMsg().toByteArray()), callback);
        } else if (toCoreNotification.hasQueueUpdateMsg()) {
            TransportProtos.QueueUpdateMsg queue = toCoreNotification.getQueueUpdateMsg();
            partitionService.updateQueue(queue);
            callback.onSuccess();
        } else if (toCoreNotification.hasQueueDeleteMsg()) {
            TransportProtos.QueueDeleteMsg queue = toCoreNotification.getQueueDeleteMsg();
            partitionService.removeQueue(queue);
            callback.onSuccess();
        } else if (toCoreNotification.hasVcResponseMsg()) {
            vcQueueService.processResponse(toCoreNotification.getVcResponseMsg());
            callback.onSuccess();
        }
        if (statsEnabled) {
            stats.log(toCoreNotification);
        }
    }

    private void launchUsageStatsConsumer() {
        usageStatsExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgs = usageStatsConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    @NotNull ConcurrentMap<UUID, TbProtoQueueMsg<ToUsageStatsServiceMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    @NotNull CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    @NotNull TbPackProcessingContext<TbProtoQueueMsg<ToUsageStatsServiceMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    pendingMap.forEach((id, msg) -> {
                        log.trace("[{}] Creating usage stats callback for message: {}", id, msg.getValue());
                        @NotNull TbCallback callback = new TbPackCallback<>(id, ctx);
                        try {
                            handleUsageStats(msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process usage stats: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process usage stats: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process usage stats: {}", id, msg.getValue()));
                    }
                    usageStatsConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new usage stats", e2);
                        }
                    }
                }
            }
            log.info("TB Usage Stats Consumer stopped.");
        });
    }

    private void launchOtaPackageUpdateNotificationConsumer() {
        long maxProcessingTimeoutPerRecord = firmwarePackInterval / firmwarePackSize;
        firmwareStatesExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> msgs = firmwareStatesConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    long timeToSleep = maxProcessingTimeoutPerRecord;
                    for (@NotNull TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg : msgs) {
                        try {
                            long startTime = System.currentTimeMillis();
                            boolean isSuccessUpdate = handleOtaPackageUpdates(msg);
                            long endTime = System.currentTimeMillis();
                            long spentTime = endTime - startTime;
                            timeToSleep = timeToSleep - spentTime;
                            if (isSuccessUpdate) {
                                if (timeToSleep > 0) {
                                    log.debug("Spent time per record is: [{}]!", spentTime);
                                    Thread.sleep(timeToSleep);
                                    timeToSleep = 0;
                                }
                                timeToSleep += maxProcessingTimeoutPerRecord;
                            }
                        } catch (Throwable e) {
                            log.warn("Failed to process firmware update msg: {}", msg, e);
                        }
                    }
                    firmwareStatesConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new firmware updates", e2);
                        }
                    }
                }
            }
            log.info("TB Ota Package States Consumer stopped.");
        });
    }

    private void handleUsageStats(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        statsService.process(msg, callback);
    }

    private boolean handleOtaPackageUpdates(@NotNull TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg) {
        return firmwareStateService.process(msg.getValue());
    }

    private void forwardToCoreRpcService(@NotNull FromDeviceRPCResponseProto proto, @NotNull TbCallback callback) {
        RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
        @NotNull FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                , proto.getResponse(), error);
        tbCoreDeviceRpcService.processRpcResponseFromRuleEngine(response);
        callback.onSuccess();
    }

    @Scheduled(fixedDelayString = "${queue.core.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void forwardToLocalSubMgrService(@NotNull LocalSubscriptionServiceMsgProto msg, @NotNull TbCallback callback) {
        if (msg.hasSubUpdate()) {
            localSubscriptionService.onSubscriptionUpdate(msg.getSubUpdate().getSessionId(), TbSubscriptionUtils.fromProto(msg.getSubUpdate()), callback);
        } else if (msg.hasAlarmSubUpdate()) {
            localSubscriptionService.onSubscriptionUpdate(msg.getAlarmSubUpdate().getSessionId(), TbSubscriptionUtils.fromProto(msg.getAlarmSubUpdate()), callback);
        } else {
            throwNotHandled(msg, callback);
        }
    }

    private void forwardToSubMgrService(@NotNull SubscriptionMgrMsgProto msg, @NotNull TbCallback callback) {
        if (msg.hasAttributeSub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getAttributeSub()), callback);
        } else if (msg.hasTelemetrySub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getTelemetrySub()), callback);
        } else if (msg.hasAlarmSub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getAlarmSub()), callback);
        } else if (msg.hasSubClose()) {
            TbSubscriptionCloseProto closeProto = msg.getSubClose();
            subscriptionManagerService.cancelSubscription(closeProto.getSessionId(), closeProto.getSubscriptionId(), callback);
        } else if (msg.hasTsUpdate()) {
            TbTimeSeriesUpdateProto proto = msg.getTsUpdate();
            subscriptionManagerService.onTimeSeriesUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    TbSubscriptionUtils.toTsKvEntityList(proto.getDataList()), callback);
        } else if (msg.hasAttrUpdate()) {
            TbAttributeUpdateProto proto = msg.getAttrUpdate();
            subscriptionManagerService.onAttributesUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), TbSubscriptionUtils.toAttributeKvList(proto.getDataList()), callback);
        } else if (msg.hasAttrDelete()) {
            TbAttributeDeleteProto proto = msg.getAttrDelete();
            subscriptionManagerService.onAttributesDelete(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), proto.getKeysList(), proto.getNotifyDevice(), callback);
        } else if (msg.hasTsDelete()) {
            TbTimeSeriesDeleteProto proto = msg.getTsDelete();
            subscriptionManagerService.onTimeSeriesDelete(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getKeysList(), callback);
        } else if (msg.hasAlarmUpdate()) {
            TbAlarmUpdateProto proto = msg.getAlarmUpdate();
            subscriptionManagerService.onAlarmUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), Alarm.class), callback);
        } else if (msg.hasAlarmDelete()) {
            TbAlarmDeleteProto proto = msg.getAlarmDelete();
            subscriptionManagerService.onAlarmDeleted(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), Alarm.class), callback);
        } else {
            throwNotHandled(msg, callback);
        }
        if (statsEnabled) {
            stats.log(msg);
        }
    }

    private void forwardToStateService(DeviceStateServiceMsgProto deviceStateServiceMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceStateServiceMsg);
        }
        stateService.onQueueMsg(deviceStateServiceMsg, callback);
    }

    private void forwardToStateService(@NotNull TransportProtos.DeviceActivityProto deviceActivityMsg, @NotNull TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceActivityMsg);
        }
        @NotNull TenantId tenantId = TenantId.fromUUID(new UUID(deviceActivityMsg.getTenantIdMSB(), deviceActivityMsg.getTenantIdLSB()));
        @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceActivityMsg.getDeviceIdMSB(), deviceActivityMsg.getDeviceIdLSB()));
        try {
            stateService.onDeviceActivity(tenantId, deviceId, deviceActivityMsg.getLastActivityTime());
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(new RuntimeException("Failed update device activity for device [" + deviceId.getId() + "]!", e));
        }
    }

    private void forwardToEdgeNotificationService(EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(edgeNotificationMsg);
        }
        edgeNotificationService.pushNotificationToEdge(edgeNotificationMsg, callback);
    }

    private void forwardToDeviceActor(@NotNull TransportToDeviceActorMsg toDeviceActorMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(toDeviceActorMsg);
        }
        actorContext.tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback));
    }

    private void forwardToAppActor(UUID id, @NotNull Optional<TbActorMsg> actorMsg, @NotNull TbCallback callback) {
        if (actorMsg.isPresent()) {
            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
            actorContext.tell(actorMsg.get());
        }
        callback.onSuccess();
    }

    private void throwNotHandled(Object msg, @NotNull TbCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }

    @Override
    protected void stopMainConsumers() {
        if (mainConsumer != null) {
            mainConsumer.unsubscribe();
        }
        if (usageStatsConsumer != null) {
            usageStatsConsumer.unsubscribe();
        }
        if (firmwareStatesConsumer != null) {
            firmwareStatesConsumer.unsubscribe();
        }
    }

}
