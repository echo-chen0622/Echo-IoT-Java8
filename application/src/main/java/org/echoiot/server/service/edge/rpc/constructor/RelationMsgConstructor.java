package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.gen.edge.v1.RelationUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class RelationMsgConstructor {

    @NotNull
    public RelationUpdateMsg constructRelationUpdatedMsg(UpdateMsgType msgType, @NotNull EntityRelation entityRelation) {
        RelationUpdateMsg.Builder builder = RelationUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setFromIdMSB(entityRelation.getFrom().getId().getMostSignificantBits())
                .setFromIdLSB(entityRelation.getFrom().getId().getLeastSignificantBits())
                .setFromEntityType(entityRelation.getFrom().getEntityType().name())
                .setToIdMSB(entityRelation.getTo().getId().getMostSignificantBits())
                .setToIdLSB(entityRelation.getTo().getId().getLeastSignificantBits())
                .setToEntityType(entityRelation.getTo().getEntityType().name())
                .setType(entityRelation.getType());
        if (entityRelation.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(entityRelation.getAdditionalInfo()));
        }
        if (entityRelation.getTypeGroup() != null) {
            builder.setTypeGroup(entityRelation.getTypeGroup().name());
        }
        return builder.build();
    }
}
