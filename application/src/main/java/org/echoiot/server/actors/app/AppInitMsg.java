package org.echoiot.server.actors.app;

import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

public class AppInitMsg implements TbActorMsg {

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.APP_INIT_MSG;
    }
}
