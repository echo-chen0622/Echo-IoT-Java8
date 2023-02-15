package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
public class WidgetsBundleMsgConstructor {

    public WidgetsBundleUpdateMsg constructWidgetsBundleUpdateMsg(UpdateMsgType msgType, WidgetsBundle widgetsBundle) {
        WidgetsBundleUpdateMsg.Builder builder = WidgetsBundleUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(widgetsBundle.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetsBundle.getId().getId().getLeastSignificantBits())
                .setTitle(widgetsBundle.getTitle())
                .setAlias(widgetsBundle.getAlias());
        if (widgetsBundle.getImage() != null) {
            builder.setImage(ByteString.copyFrom(widgetsBundle.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (widgetsBundle.getDescription() != null) {
            builder.setDescription(widgetsBundle.getDescription());
        }
        if (widgetsBundle.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }

    public WidgetsBundleUpdateMsg constructWidgetsBundleDeleteMsg(WidgetsBundleId widgetsBundleId) {
        return WidgetsBundleUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetsBundleId.getId().getMostSignificantBits())
                .setIdLSB(widgetsBundleId.getId().getLeastSignificantBits())
                .build();
    }
}
