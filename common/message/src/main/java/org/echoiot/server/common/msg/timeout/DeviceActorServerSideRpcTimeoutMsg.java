package org.echoiot.server.common.msg.timeout;

import org.echoiot.server.common.msg.MsgType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
public final class DeviceActorServerSideRpcTimeoutMsg extends TimeoutMsg<Integer> {

    public DeviceActorServerSideRpcTimeoutMsg(Integer id, long timeout) {
        super(id, timeout);
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG;
    }
}
