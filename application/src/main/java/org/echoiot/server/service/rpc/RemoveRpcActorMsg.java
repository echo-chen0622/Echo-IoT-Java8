package org.echoiot.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@RequiredArgsConstructor
public class RemoveRpcActorMsg implements ToDeviceActorNotificationMsg {

    @NotNull
    @Getter
    private final TenantId tenantId;
    @NotNull
    @Getter
    private final DeviceId deviceId;

    @NotNull
    @Getter
    private final UUID requestId;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.REMOVE_RPC_TO_DEVICE_ACTOR_MSG;
    }
}
