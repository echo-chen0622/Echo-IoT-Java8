package org.thingsboard.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

import java.util.UUID;

@ToString
@RequiredArgsConstructor
public class RemoveRpcActorMsg implements ToDeviceActorNotificationMsg {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final DeviceId deviceId;

    @Getter
    private final UUID requestId;

    @Override
    public MsgType getMsgType() {
        return MsgType.REMOVE_RPC_TO_DEVICE_ACTOR_MSG;
    }
}
