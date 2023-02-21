package org.echoiot.server.service.queue;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.msg.DeviceEdgeUpdateMsg;
import org.echoiot.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.echoiot.server.common.msg.edge.EdgeEventUpdateMsg;
import org.echoiot.server.common.msg.edge.FromEdgeSyncResponse;
import org.echoiot.server.common.msg.edge.ToEdgeSyncRequest;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.MultipleTbQueueCallbackWrapper;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.service.gateway_device.GatewayNotificationsService;
import org.echoiot.server.service.ota.OtaPackageStateService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbClusterService implements TbClusterService {

    @Value("${cluster.stats.enabled:false}")
    private boolean statsEnabled;
    @Value("${edges.enabled:true}")
    protected boolean edgesEnabled;

    private final AtomicInteger toCoreMsgs = new AtomicInteger(0);
    private final AtomicInteger toCoreNfs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineMsgs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineNfs = new AtomicInteger(0);
    private final AtomicInteger toTransportNfs = new AtomicInteger(0);

    @Resource
    @Lazy
    private PartitionService partitionService;

    @Resource
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Resource
    @Lazy
    private OtaPackageStateService otaPackageStateService;

    @NotNull
    private final NotificationsTopicService notificationsTopicService;
    @NotNull
    private final DataDecodingEncodingService encodingService;
    @NotNull
    private final TbDeviceProfileCache deviceProfileCache;
    @NotNull
    private final TbAssetProfileCache assetProfileCache;
    @NotNull
    private final GatewayNotificationsService gatewayNotificationsService;

    @Override
    public void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ToCoreMsg msg, TbQueueCallback callback) {
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(@NotNull ToDeviceActorNotificationMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        byte[] msgBytes = encodingService.encode(msg);
        ToCoreMsg toCoreMsg = ToCoreMsg.newBuilder().setToDeviceActorNotificationMsg(ByteString.copyFrom(msgBytes)).build();
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msg.getDeviceId().getId(), toCoreMsg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToVersionControl(@NotNull TenantId tenantId, TransportProtos.ToVersionControlServiceMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_VC_EXECUTOR, tenantId, tenantId);
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getTbVersionControlMsgProducer().send(tpi, new TbProtoQueueMsg<>(tenantId.getId(), msg), callback);
        //TODO: Echo
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushNotificationToCore(String serviceId, @NotNull FromDeviceRpcResponse response, TbQueueCallback callback) {
        @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback) {
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(@Nullable TenantId tenantId, @NotNull EntityId entityId, TbMsg tbMsg, TbQueueCallback callback) {
        if (tenantId == null || tenantId.isNullUid()) {
            if (entityId.getEntityType().equals(EntityType.TENANT)) {
                tenantId = TenantId.fromUUID(entityId.getId());
            } else {
                log.warn("[{}][{}] Received invalid message: {}", tenantId, entityId, tbMsg);
                return;
            }
        } else {
            if (entityId.getEntityType().equals(EntityType.DEVICE)) {
                tbMsg = transformMsg(tbMsg, deviceProfileCache.get(tenantId, new DeviceId(entityId.getId())));
            } else if (entityId.getEntityType().equals(EntityType.DEVICE_PROFILE)) {
                tbMsg = transformMsg(tbMsg, deviceProfileCache.get(tenantId, new DeviceProfileId(entityId.getId())));
            } else if (entityId.getEntityType().equals(EntityType.ASSET)) {
                tbMsg = transformMsg(tbMsg, assetProfileCache.get(tenantId, new AssetId(entityId.getId())));
            } else if (entityId.getEntityType().equals(EntityType.ASSET_PROFILE)) {
                tbMsg = transformMsg(tbMsg, assetProfileCache.get(tenantId, new AssetProfileId(entityId.getId())));
            }
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, entityId);
        log.trace("PUSHING msg: {} to:{}", tbMsg, tpi);
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    private TbMsg transformMsg(TbMsg tbMsg, @Nullable DeviceProfile deviceProfile) {
        if (deviceProfile != null) {
            RuleChainId targetRuleChainId = deviceProfile.getDefaultRuleChainId();
            String targetQueueName = deviceProfile.getDefaultQueueName();
            tbMsg = transformMsg(tbMsg, targetRuleChainId, targetQueueName);
        }
        return tbMsg;
    }

    private TbMsg transformMsg(TbMsg tbMsg, @Nullable AssetProfile assetProfile) {
        if (assetProfile != null) {
            RuleChainId targetRuleChainId = assetProfile.getDefaultRuleChainId();
            String targetQueueName = assetProfile.getDefaultQueueName();
            tbMsg = transformMsg(tbMsg, targetRuleChainId, targetQueueName);
        }
        return tbMsg;
    }

    private TbMsg transformMsg(@NotNull TbMsg tbMsg, @Nullable RuleChainId targetRuleChainId, @Nullable String targetQueueName) {
        boolean isRuleChainTransform = targetRuleChainId != null && !targetRuleChainId.equals(tbMsg.getRuleChainId());
        boolean isQueueTransform = targetQueueName != null && !targetQueueName.equals(tbMsg.getQueueName());

        if (isRuleChainTransform && isQueueTransform) {
            tbMsg = TbMsg.transformMsg(tbMsg, targetRuleChainId, targetQueueName);
        } else if (isRuleChainTransform) {
            tbMsg = TbMsg.transformMsg(tbMsg, targetRuleChainId);
        } else if (isQueueTransform) {
            tbMsg = TbMsg.transformMsg(tbMsg, targetQueueName);
        }
        return tbMsg;
    }

    @Override
    public void pushNotificationToRuleEngine(String serviceId, @NotNull FromDeviceRpcResponse response, TbQueueCallback callback) {
        @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToRuleEngineNotificationMsg msg = ToRuleEngineNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toRuleEngineNfs.incrementAndGet();
    }

    @Override
    public void pushNotificationToTransport(@Nullable String serviceId, ToTransportMsg response, @Nullable TbQueueCallback callback) {
        if (serviceId == null || serviceId.isEmpty()) {
            log.trace("pushNotificationToTransport: skipping message without serviceId [{}], (ToTransportMsg) response [{}]", serviceId, response);
            if (callback != null) {
                callback.onSuccess(null); //callback that message already sent, no useful payload expected
            }
            return;
        }
        @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), response), callback);
        toTransportNfs.incrementAndGet();
    }

    @Override
    public void broadcastEntityStateChangeEvent(TenantId tenantId, @NotNull EntityId entityId, ComponentLifecycleEvent state) {
        log.trace("[{}] Processing {} state change event: {}", tenantId, entityId.getEntityType(), state);
        broadcast(new ComponentLifecycleMsg(tenantId, entityId, state));
    }

    @Override
    public void onDeviceProfileChange(@NotNull DeviceProfile deviceProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile, callback);
    }

    @Override
    public void onTenantProfileChange(@NotNull TenantProfile tenantProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenantProfile.getId(), tenantProfile, callback);
    }

    @Override
    public void onTenantChange(@NotNull Tenant tenant, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenant.getId(), tenant, callback);
    }

    @Override
    public void onApiStateChange(@NotNull ApiUsageState apiUsageState, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(apiUsageState.getTenantId(), apiUsageState.getId(), apiUsageState, callback);
        broadcast(new ComponentLifecycleMsg(apiUsageState.getTenantId(), apiUsageState.getId(), ComponentLifecycleEvent.UPDATED));
    }

    @Override
    public void onDeviceProfileDelete(@NotNull DeviceProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(entity.getTenantId(), entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onTenantProfileDelete(@NotNull TenantProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onTenantDelete(@NotNull Tenant entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onDeviceDeleted(@NotNull Device device, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(device.getTenantId(), device.getId(), device.getName(), callback);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), false, false, true);
        broadcastEntityStateChangeEvent(device.getTenantId(), device.getId(), ComponentLifecycleEvent.DELETED);
    }

    @Override
    public void onResourceChange(@NotNull TbResource resource, TbQueueCallback callback) {
        TenantId tenantId = resource.getTenantId();
        log.trace("[{}][{}][{}] Processing change resource", tenantId, resource.getResourceType(), resource.getResourceKey());
        TransportProtos.ResourceUpdateMsg resourceUpdateMsg = TransportProtos.ResourceUpdateMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceUpdateMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    @Override
    public void onResourceDeleted(@NotNull TbResource resource, TbQueueCallback callback) {
        log.trace("[{}] Processing delete resource", resource);
        TransportProtos.ResourceDeleteMsg resourceUpdateMsg = TransportProtos.ResourceDeleteMsg.newBuilder()
                .setTenantIdMSB(resource.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(resource.getTenantId().getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceDeleteMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    public <T> void broadcastEntityChangeToTransport(TenantId tenantId, @NotNull EntityId entityid, T entity, TbQueueCallback callback) {
        String entityName = (entity instanceof HasName) ? ((HasName) entity).getName() : entity.getClass().getName();
        log.trace("[{}][{}][{}] Processing [{}] change event", tenantId, entityid.getEntityType(), entityid.getId(), entityName);
        TransportProtos.EntityUpdateMsg entityUpdateMsg = TransportProtos.EntityUpdateMsg.newBuilder()
                .setEntityType(entityid.getEntityType().name())
                .setData(ByteString.copyFrom(encodingService.encode(entity))).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityUpdateMsg(entityUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    private void broadcastEntityDeleteToTransport(TenantId tenantId, @NotNull EntityId entityId, String name, TbQueueCallback callback) {
        log.trace("[{}][{}][{}] Processing [{}] delete event", tenantId, entityId.getEntityType(), entityId.getId(), name);
        TransportProtos.EntityDeleteMsg entityDeleteMsg = TransportProtos.EntityDeleteMsg.newBuilder()
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityDeleteMsg(entityDeleteMsg).build();
        broadcast(transportMsg, callback);
    }

    private void broadcast(ToTransportMsg transportMsg, @Nullable TbQueueCallback callback) {
        TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> toTransportNfProducer = producerProvider.getTransportNotificationsMsgProducer();
        Set<String> tbTransportServices = partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT);
        @Nullable TbQueueCallback proxyCallback = callback != null ? new MultipleTbQueueCallbackWrapper(tbTransportServices.size(), callback) : null;
        for (String transportServiceId : tbTransportServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportServiceId);
            toTransportNfProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), proxyCallback);
            toTransportNfs.incrementAndGet();
        }
    }

    @Override
    public void onEdgeEventUpdate(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("[{}] Processing edge {} event update ", tenantId, edgeId);
        @NotNull EdgeEventUpdateMsg msg = new EdgeEventUpdateMsg(tenantId, edgeId);
        byte[] msgBytes = encodingService.encode(msg);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setEdgeEventUpdateMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(edgeId, toCoreMsg);
    }

    @Override
    public void pushEdgeSyncRequestToCore(@NotNull ToEdgeSyncRequest toEdgeSyncRequest) {
        log.trace("[{}] Processing edge sync request {} ", toEdgeSyncRequest.getTenantId(), toEdgeSyncRequest);
        byte[] msgBytes = encodingService.encode(toEdgeSyncRequest);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToEdgeSyncRequestMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(toEdgeSyncRequest.getEdgeId(), toCoreMsg);
    }

    @Override
    public void pushEdgeSyncResponseToCore(@NotNull FromEdgeSyncResponse fromEdgeSyncResponse) {
        log.trace("[{}] Processing edge sync response {}", fromEdgeSyncResponse.getTenantId(), fromEdgeSyncResponse);
        byte[] msgBytes = encodingService.encode(fromEdgeSyncResponse);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setFromEdgeSyncResponseMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(fromEdgeSyncResponse.getEdgeId(), toCoreMsg);
    }

    private void pushEdgeSyncMsgToCore(@NotNull EdgeId edgeId, ToCoreNotificationMsg toCoreMsg) {
        TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        for (String serviceId : tbCoreServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(edgeId.getId(), toCoreMsg), null);
            toCoreNfs.incrementAndGet();
        }
    }

    private void broadcast(@NotNull ComponentLifecycleMsg msg) {
        byte[] msgBytes = encodingService.encode(msg);
        TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> toRuleEngineProducer = producerProvider.getRuleEngineNotificationsMsgProducer();
        Set<String> tbRuleEngineServices = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        EntityType entityType = msg.getEntityId().getEntityType();
        if (entityType.equals(EntityType.TENANT)
                || entityType.equals(EntityType.TENANT_PROFILE)
                || entityType.equals(EntityType.DEVICE_PROFILE)
                || entityType.equals(EntityType.ASSET_PROFILE)
                || entityType.equals(EntityType.API_USAGE_STATE)
                || (entityType.equals(EntityType.DEVICE) && msg.getEvent() == ComponentLifecycleEvent.UPDATED)
                || entityType.equals(EntityType.ENTITY_VIEW)
                || entityType.equals(EntityType.EDGE)) {
            TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
            Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
            for (String serviceId : tbCoreServices) {
                @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
                toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toCoreMsg), null);
                toCoreNfs.incrementAndGet();
            }
            // No need to push notifications twice
            tbRuleEngineServices.removeAll(tbCoreServices);
        }
        for (String serviceId : tbRuleEngineServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
            ToRuleEngineNotificationMsg toRuleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
            toRuleEngineProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toRuleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
    }

    @Scheduled(fixedDelayString = "${cluster.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int toCoreMsgCnt = toCoreMsgs.getAndSet(0);
            int toCoreNfsCnt = toCoreNfs.getAndSet(0);
            int toRuleEngineMsgsCnt = toRuleEngineMsgs.getAndSet(0);
            int toRuleEngineNfsCnt = toRuleEngineNfs.getAndSet(0);
            int toTransportNfsCnt = toTransportNfs.getAndSet(0);
            if (toCoreMsgCnt > 0 || toCoreNfsCnt > 0 || toRuleEngineMsgsCnt > 0 || toRuleEngineNfsCnt > 0 || toTransportNfsCnt > 0) {
                log.info("To TbCore: [{}] messages [{}] notifications; To TbRuleEngine: [{}] messages [{}] notifications; To Transport: [{}] notifications",
                        toCoreMsgCnt, toCoreNfsCnt, toRuleEngineMsgsCnt, toRuleEngineNfsCnt, toTransportNfsCnt);
            }
        }
    }

    private void sendDeviceStateServiceEvent(@NotNull TenantId tenantId, @NotNull DeviceId deviceId, boolean added, boolean updated, boolean deleted) {
        TransportProtos.DeviceStateServiceMsgProto.Builder builder = TransportProtos.DeviceStateServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setDeviceIdMSB(deviceId.getId().getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        TransportProtos.DeviceStateServiceMsgProto msg = builder.build();
        pushMsgToCore(tenantId, deviceId, TransportProtos.ToCoreMsg.newBuilder().setDeviceStateServiceMsg(msg).build(), null);
    }

    @Override
    public void onDeviceUpdated(@NotNull Device device, Device old) {
        onDeviceUpdated(device, old, true);
    }

    @Override
    public void onDeviceUpdated(@NotNull Device device, @Nullable Device old, boolean notifyEdge) {
        var created = old == null;
        broadcastEntityChangeToTransport(device.getTenantId(), device.getId(), device, null);
        if (old != null) {
            boolean deviceNameChanged = !device.getName().equals(old.getName());
            if (deviceNameChanged) {
                gatewayNotificationsService.onDeviceUpdated(device, old);
            }
            if (deviceNameChanged || !device.getType().equals(old.getType())) {
                pushMsgToCore(new DeviceNameOrTypeUpdateMsg(device.getTenantId(), device.getId(), device.getName(), device.getType()), null);
            }
        }
        broadcastEntityStateChangeEvent(device.getTenantId(), device.getId(), created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), created, !created, false);
        otaPackageStateService.update(device, old);
        if (!created && notifyEdge) {
            sendNotificationMsgToEdge(device.getTenantId(), null, device.getId(), null, null, EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public void sendNotificationMsgToEdge(@NotNull TenantId tenantId, @Nullable EdgeId edgeId, @Nullable EntityId entityId, @Nullable String body, @Nullable EdgeEventType type, @NotNull EdgeEventActionType action) {
        if (!edgesEnabled) {
            return;
        }
        if (type == null) {
            if (entityId != null) {
                type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
            } else {
                log.trace("[{}] entity id and type are null. Ignoring this notification", tenantId);
                return;
            }
            if (type == null) {
                log.trace("[{}] edge event type is null. Ignoring this notification [{}]", tenantId, entityId);
                return;
            }
        }
        TransportProtos.EdgeNotificationMsgProto.Builder builder = TransportProtos.EdgeNotificationMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setType(type.name());
        builder.setAction(action.name());
        if (entityId != null) {
            builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
            builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
            builder.setEntityType(entityId.getEntityType().name());
        }
        if (edgeId != null) {
            builder.setEdgeIdMSB(edgeId.getId().getMostSignificantBits());
            builder.setEdgeIdLSB(edgeId.getId().getLeastSignificantBits());
        }
        if (body != null) {
            builder.setBody(body);
        }
        TransportProtos.EdgeNotificationMsgProto msg = builder.build();
        log.trace("[{}] sending notification to edge service {}", tenantId.getId(), msg);
        pushMsgToCore(tenantId, entityId != null ? entityId : tenantId, TransportProtos.ToCoreMsg.newBuilder().setEdgeNotificationMsg(msg).build(), null);

        if (entityId != null && EntityType.DEVICE.equals(entityId.getEntityType())) {
            pushDeviceUpdateMessage(tenantId, edgeId, entityId, action);
        }
    }

    private void pushDeviceUpdateMessage(TenantId tenantId, EdgeId edgeId, @NotNull EntityId entityId, @NotNull EdgeEventActionType action) {
        log.trace("{} Going to send edge update notification for device actor, device id {}, edge id {}", tenantId, entityId, edgeId);
        switch (action) {
            case ASSIGNED_TO_EDGE:
                pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), edgeId), null);
                break;
            case UNASSIGNED_FROM_EDGE:
                pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), null), null);
                break;
        }
    }

    @Override
    public void onQueueChange(@NotNull Queue queue) {
        log.trace("[{}][{}] Processing queue change [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueUpdateMsg queueUpdateMsg = TransportProtos.QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .setQueueTopic(queue.getTopic())
                .setPartitions(queue.getPartitions())
                .build();

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    @Override
    public void onQueueDelete(@NotNull Queue queue) {
        log.trace("[{}][{}] Processing queue delete [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueDeleteMsg queueDeleteMsg = TransportProtos.QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .build();

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    private void doSendQueueNotifications(ToRuleEngineNotificationMsg ruleEngineMsg, ToCoreNotificationMsg coreMsg, ToTransportMsg transportMsg) {
        Set<String> tbRuleEngineServices = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        Set<String> tbTransportServices = partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT);
        // No need to push notifications twice
        tbTransportServices.removeAll(tbCoreServices);
        tbCoreServices.removeAll(tbRuleEngineServices);

        for (String ruleEngineServiceId : tbRuleEngineServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngineServiceId);
            producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ruleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
        for (String coreServiceId : tbCoreServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, coreServiceId);
            producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), coreMsg), null);
            toCoreNfs.incrementAndGet();
        }
        for (String transportServiceId : tbTransportServices) {
            @NotNull TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportServiceId);
            producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), null);
            toTransportNfs.incrementAndGet();
        }
    }
}
