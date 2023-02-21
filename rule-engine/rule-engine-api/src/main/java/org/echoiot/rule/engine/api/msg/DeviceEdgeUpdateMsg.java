package org.echoiot.rule.engine.api.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class DeviceEdgeUpdateMsg implements ToDeviceActorNotificationMsg {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    @NotNull
    private final EdgeId edgeId;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
