package org.echoiot.rule.engine.api.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class DeviceNameOrTypeUpdateMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -5738949227650536685L;

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    @NotNull
    private final String deviceName;
    @NotNull
    private final String deviceType;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
