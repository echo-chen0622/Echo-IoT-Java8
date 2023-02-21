package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetTypeId;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.edge.v1.WidgetTypeUpdateMsg;

@Component
@TbCoreComponent
public class WidgetTypeMsgConstructor {

    @NotNull
    public WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, @NotNull WidgetTypeDetails widgetTypeDetails) {
        @NotNull WidgetTypeUpdateMsg.Builder builder = WidgetTypeUpdateMsg.newBuilder()
                                                                          .setMsgType(msgType)
                                                                          .setIdMSB(widgetTypeDetails.getId().getId().getMostSignificantBits())
                                                                          .setIdLSB(widgetTypeDetails.getId().getId().getLeastSignificantBits());
        if (widgetTypeDetails.getBundleAlias() != null) {
            builder.setBundleAlias(widgetTypeDetails.getBundleAlias());
        }
        if (widgetTypeDetails.getAlias() != null) {
            builder.setAlias(widgetTypeDetails.getAlias());
        }
        if (widgetTypeDetails.getName() != null) {
            builder.setName(widgetTypeDetails.getName());
        }
        if (widgetTypeDetails.getDescriptor() != null) {
            builder.setDescriptorJson(JacksonUtil.toString(widgetTypeDetails.getDescriptor()));
        }
        if (widgetTypeDetails.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
            builder.setIsSystem(true);
        }
        if (widgetTypeDetails.getImage() != null) {
            builder.setImage(widgetTypeDetails.getImage());
        }
        if (widgetTypeDetails.getDescription() != null) {
            builder.setDescription(widgetTypeDetails.getDescription());
        }
        return builder.build();
    }

    @NotNull
    public WidgetTypeUpdateMsg constructWidgetTypeDeleteMsg(@NotNull WidgetTypeId widgetTypeId) {
        return WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetTypeId.getId().getMostSignificantBits())
                .setIdLSB(widgetTypeId.getId().getLeastSignificantBits())
                .build();
    }
}
