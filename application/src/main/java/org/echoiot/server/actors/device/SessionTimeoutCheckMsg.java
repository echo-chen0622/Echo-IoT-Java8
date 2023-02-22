package org.echoiot.server.actors.device;

import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;

/**
 * Created by Echo on 29.10.18.
 */
public class SessionTimeoutCheckMsg implements TbActorMsg {

    private static final SessionTimeoutCheckMsg INSTANCE = new SessionTimeoutCheckMsg();

    private SessionTimeoutCheckMsg() {
    }

    public static SessionTimeoutCheckMsg instance() {
        return INSTANCE;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.SESSION_TIMEOUT_MSG;
    }
}
