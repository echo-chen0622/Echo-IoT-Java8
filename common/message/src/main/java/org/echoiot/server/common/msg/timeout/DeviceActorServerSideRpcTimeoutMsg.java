package org.echoiot.server.common.msg.timeout;

import org.echoiot.server.common.msg.MsgType;

/**
 * @author Echo
 */
public final class DeviceActorServerSideRpcTimeoutMsg extends TimeoutMsg<Integer> {

    public DeviceActorServerSideRpcTimeoutMsg(Integer id, long timeout) {
        super(id, timeout);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG;
    }
}
