package org.echoiot.server.actors.stats;

import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

public final class StatsPersistTick implements TbActorMsg {
    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.STATS_PERSIST_TICK_MSG;
    }
}
