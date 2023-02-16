package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.v1.EdgeEntityType;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

@Component
@TbCoreComponent
public class EntityViewMsgConstructor {

    public EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView) {
        EdgeEntityType entityType;
        switch (entityView.getEntityId().getEntityType()) {
            case EntityType.DEVICE:
                entityType = EdgeEntityType.DEVICE;
                break;
            case EntityType.ASSET:
                entityType = EdgeEntityType.ASSET;
                break;
            default:
                throw new RuntimeException("Unsupported entity type [" + entityView.getEntityId().getEntityType() + "]");
        }
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(entityView.getId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getId().getId().getLeastSignificantBits())
                .setName(entityView.getName())
                .setType(entityView.getType())
                .setEntityIdMSB(entityView.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(entityView.getEntityId().getId().getLeastSignificantBits())
                .setEntityType(entityType);
        if (entityView.getCustomerId() != null) {
            builder.setCustomerIdMSB(entityView.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(entityView.getCustomerId().getId().getLeastSignificantBits());
        }
        if (entityView.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(entityView.getAdditionalInfo()));
        }
        return builder.build();
    }

    public EntityViewUpdateMsg constructEntityViewDeleteMsg(EntityViewId entityViewId) {
        return EntityViewUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(entityViewId.getId().getMostSignificantBits())
                .setIdLSB(entityViewId.getId().getLeastSignificantBits()).build();
    }
}
