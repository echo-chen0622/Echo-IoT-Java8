package org.thingsboard.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;

@ToString
@RequiredArgsConstructor
public class FromDeviceRpcResponseActorMsg implements ToDeviceActorNotificationMsg {

    @Getter
    private final Integer requestId;
    @Getter
    private final TenantId tenantId;
    @Getter
    private final DeviceId deviceId;

    @Getter
    private final FromDeviceRpcResponse msg;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG;
    }
}
