package org.echoiot.server.service.edge.rpc.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.common.transport.util.JsonUtils;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.gen.edge.v1.AttributeDeleteMsg;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.EntityDataProto;
import org.echoiot.server.gen.transport.TransportProtos;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@TbCoreComponent
public class TelemetryEdgeProcessor extends BaseEdgeProcessor {

    private final Gson gson = new Gson();

    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;

    @PostConstruct
    public void init() {
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
    }

    public List<ListenableFuture<Void>> processTelemetryFromEdge(TenantId tenantId, EntityDataProto entityData) {
        log.trace("[{}] processTelemetryFromEdge [{}]", tenantId, entityData);
        List<ListenableFuture<Void>> result = new ArrayList<>();
        EntityId entityId = constructEntityId(entityData.getEntityType(), entityData.getEntityIdMSB(), entityData.getEntityIdLSB());
        if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg() || entityData.hasAttributesUpdatedMsg()) && entityId != null) {
            Pair<TbMsgMetaData, CustomerId> pair = getBaseMsgMetadataAndCustomerId(tenantId, entityId);
            TbMsgMetaData metaData = pair.getKey();
            CustomerId customerId = pair.getValue();
            metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.EDGE_MSG_SOURCE);
            if (entityData.hasPostAttributesMsg()) {
                result.add(processPostAttributes(tenantId, customerId, entityId, entityData.getPostAttributesMsg(), metaData));
            }
            if (entityData.hasAttributesUpdatedMsg()) {
                metaData.putValue("scope", entityData.getPostAttributeScope());
                result.add(processAttributesUpdate(tenantId, customerId, entityId, entityData.getAttributesUpdatedMsg(), metaData));
            }
            if (entityData.hasPostTelemetryMsg()) {
                result.add(processPostTelemetry(tenantId, customerId, entityId, entityData.getPostTelemetryMsg(), metaData));
            }
            if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                DeviceId deviceId = new DeviceId(entityId.getId());

                long currentTs = System.currentTimeMillis();

                TransportProtos.DeviceActivityProto deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastActivityTime(currentTs).build();

                log.trace("[{}][{}] device activity time is going to be updated, ts {}", tenantId, deviceId, currentTs);

                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
                tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(deviceId.getId(),
                        TransportProtos.ToCoreMsg.newBuilder().setDeviceActivityMsg(deviceActivityMsg).build()), null);
            }
        }
        if (entityData.hasAttributeDeleteMsg()) {
            result.add(processAttributeDeleteMsg(tenantId, entityId, entityData.getAttributeDeleteMsg(), entityData.getEntityType()));
        }
        return result;
    }

    private Pair<TbMsgMetaData, CustomerId> getBaseMsgMetadataAndCustomerId(TenantId tenantId, EntityId entityId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        CustomerId customerId = null;
        switch (entityId.getEntityType()) {
            case DEVICE:
                Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) {
                    customerId = device.getCustomerId();
                    metaData.putValue("deviceName", device.getName());
                    metaData.putValue("deviceType", device.getType());
                }
                break;
            case ASSET:
                Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                if (asset != null) {
                    customerId = asset.getCustomerId();
                    metaData.putValue("assetName", asset.getName());
                    metaData.putValue("assetType", asset.getType());
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                if (entityView != null) {
                    customerId = entityView.getCustomerId();
                    metaData.putValue("entityViewName", entityView.getName());
                    metaData.putValue("entityViewType", entityView.getType());
                }
                break;
            case EDGE:
                Edge edge = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                if (edge != null) {
                    customerId = edge.getCustomerId();
                    metaData.putValue("edgeName", edge.getName());
                    metaData.putValue("edgeType", edge.getType());
                }
                break;
            default:
                log.debug("Using empty metadata for entityId [{}]", entityId);
                break;
        }
        return new ImmutablePair<>(metaData, customerId != null ? customerId : new CustomerId(ModelConstants.NULL_UUID));
    }

    private ListenableFuture<Void> processPostTelemetry(TenantId tenantId, CustomerId customerId, EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
            TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
            tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process post telemetry [{}]", msg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private Pair<String, RuleChainId> getDefaultQueueNameAndRuleChainId(TenantId tenantId, EntityId entityId) {
        RuleChainId ruleChainId = null;
        String queueName = null;
        if (EntityType.DEVICE.equals(entityId.getEntityType())) {
            DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, new DeviceId(entityId.getId()));
            if (deviceProfile == null) {
                log.warn("[{}] Device profile is null!", entityId);
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                queueName = deviceProfile.getDefaultQueueName();
            }
        } else if (EntityType.ASSET.equals(entityId.getEntityType())) {
            AssetProfile assetProfile = assetProfileCache.get(tenantId, new AssetId(entityId.getId()));
            if (assetProfile == null) {
                log.warn("[{}] Asset profile is null!", entityId);
            } else {
                ruleChainId = assetProfile.getDefaultRuleChainId();
                queueName = assetProfile.getDefaultQueueName();
            }
        }
        return new ImmutablePair<>(queueName, ruleChainId);
    }

    private ListenableFuture<Void> processPostAttributes(TenantId tenantId, CustomerId customerId, EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
        TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
        tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't process post attributes [{}]", msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributesUpdate(TenantId tenantId,
                                                           CustomerId customerId,
                                                           EntityId entityId,
                                                           TransportProtos.PostAttributeMsg msg,
                                                           TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(json));
        String scope = metaData.getValue("scope");
        tsSubService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                var defaultQueueAndRuleChain = getDefaultQueueNameAndRuleChainId(tenantId, entityId);
                TbMsg tbMsg = TbMsg.newMsg(defaultQueueAndRuleChain.getKey(), DataConstants.ATTRIBUTES_UPDATED, entityId,
                        customerId, metaData, gson.toJson(json), defaultQueueAndRuleChain.getValue(), null);
                tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Can't process attributes update [{}]", msg, t);
                        futureToSet.setException(t);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't process attributes update [{}]", msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributeDeleteMsg(TenantId tenantId, EntityId entityId, AttributeDeleteMsg attributeDeleteMsg,
                                                             String entityType) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributeDeleteMsg.getScope();
        List<String> attributeNames = attributeDeleteMsg.getAttributeNamesList();
        attributesService.removeAll(tenantId, entityId, scope, attributeNames);
        if (EntityType.DEVICE.name().equals(entityType)) {
            tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                    tenantId, (DeviceId) entityId, scope, attributeNames), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process attribute delete msg [{}]", attributeDeleteMsg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    public DownlinkMsg convertTelemetryEventToDownlink(EdgeEvent edgeEvent) throws JsonProcessingException {
        EntityId entityId;
        switch (edgeEvent.getType()) {
            case DEVICE:
                entityId = new DeviceId(edgeEvent.getEntityId());
                break;
            case ASSET:
                entityId = new AssetId(edgeEvent.getEntityId());
                break;
            case ENTITY_VIEW:
                entityId = new EntityViewId(edgeEvent.getEntityId());
                break;
            case DASHBOARD:
                entityId = new DashboardId(edgeEvent.getEntityId());
                break;
            case TENANT:
                entityId = TenantId.fromUUID(edgeEvent.getEntityId());
                break;
            case CUSTOMER:
                entityId = new CustomerId(edgeEvent.getEntityId());
                break;
            case USER:
                entityId = new UserId(edgeEvent.getEntityId());
                break;
            case EDGE:
                entityId = new EdgeId(edgeEvent.getEntityId());
                break;
            default:
                log.warn("Unsupported edge event type [{}]", edgeEvent);
                return null;
        }
        return constructEntityDataProtoMsg(entityId, edgeEvent.getAction(),
                JsonUtils.parse(JacksonUtil.OBJECT_MAPPER.writeValueAsString(edgeEvent.getBody())));
    }

    private DownlinkMsg constructEntityDataProtoMsg(EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = entityDataMsgConstructor.constructEntityDataMsg(entityId, actionType, entityData);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityData(entityDataProto)
                .build();
    }

}