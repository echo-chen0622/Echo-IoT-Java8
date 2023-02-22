package org.echoiot.server.actors.app;

import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;

public class AppInitMsg implements TbActorMsg {

    @Override
    public MsgType getMsgType() {
        return MsgType.APP_INIT_MSG;
    }
}
