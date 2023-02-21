package org.echoiot.server.service.edge.rpc.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.RelationUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class RelationEdgeProcessor extends BaseEdgeProcessor {

    @NotNull
    public ListenableFuture<Void> processRelationFromEdge(TenantId tenantId, @NotNull RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] onRelationUpdate [{}]", tenantId, relationUpdateMsg);
        try {
            @NotNull EntityRelation entityRelation = new EntityRelation();

            @NotNull UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
            EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
            entityRelation.setFrom(fromId);

            @NotNull UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
            EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
            entityRelation.setTo(toId);

            entityRelation.setType(relationUpdateMsg.getType());
            if (relationUpdateMsg.hasTypeGroup()) {
                entityRelation.setTypeGroup(RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()));
            }
            entityRelation.setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.readTree(relationUpdateMsg.getAdditionalInfo()));
            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (isEntityExists(tenantId, entityRelation.getTo())
                            && isEntityExists(tenantId, entityRelation.getFrom())) {
                        relationService.saveRelationAsync(tenantId, entityRelation);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    relationService.deleteRelation(tenantId, entityRelation);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
            return Futures.immediateFuture(null);
        } catch (Exception e) {
            log.error("Failed to process relation update msg [{}]", relationUpdateMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException("Failed to process relation update msg", e));
        }
    }


    private boolean isEntityExists(TenantId tenantId, @NotNull EntityId entityId) throws EchoiotException {
        switch (entityId.getEntityType()) {
            case DEVICE:
                return deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId())) != null;
            case ASSET:
                return assetService.findAssetById(tenantId, new AssetId(entityId.getId())) != null;
            case ENTITY_VIEW:
                return entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId())) != null;
            case CUSTOMER:
                return customerService.findCustomerById(tenantId, new CustomerId(entityId.getId())) != null;
            case USER:
                return userService.findUserById(tenantId, new UserId(entityId.getId())) != null;
            case DASHBOARD:
                return dashboardService.findDashboardById(tenantId, new DashboardId(entityId.getId())) != null;
            default:
                throw new EchoiotException("Unsupported entity type " + entityId.getEntityType(), EchoiotErrorCode.INVALID_ARGUMENTS);
        }
    }

    @NotNull
    public DownlinkMsg convertRelationEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        EntityRelation entityRelation = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), EntityRelation.class);
        @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        RelationUpdateMsg relationUpdateMsg = relationMsgConstructor.constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationUpdateMsg(relationUpdateMsg)
                .build();
    }

    @NotNull
    public ListenableFuture<Void> processRelationNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EntityRelation relation = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (relation.getFrom().getEntityType().equals(EntityType.EDGE) ||
                relation.getTo().getEntityType().equals(EntityType.EDGE)) {
            return Futures.immediateFuture(null);
        }

        @NotNull Set<EdgeId> uniqueEdgeIds = new HashSet<>();
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getTo()));
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getFrom()));
        if (uniqueEdgeIds.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EdgeId edgeId : uniqueEdgeIds) {
            futures.add(saveEdgeEvent(tenantId,
                                      edgeId,
                                      EdgeEventType.RELATION,
                                      EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                                      null,
                                      JacksonUtil.OBJECT_MAPPER.valueToTree(relation)));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }
}
