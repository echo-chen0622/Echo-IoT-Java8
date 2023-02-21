package org.echoiot.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;
import org.jetbrains.annotations.NotNull;

@ToString
@RequiredArgsConstructor
public class FromDeviceRpcResponseActorMsg implements ToDeviceActorNotificationMsg {

    @NotNull
    @Getter
    private final Integer requestId;
    @NotNull
    @Getter
    private final TenantId tenantId;
    @NotNull
    @Getter
    private final DeviceId deviceId;

    @NotNull
    @Getter
    private final FromDeviceRpcResponse msg;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG;
    }
}
