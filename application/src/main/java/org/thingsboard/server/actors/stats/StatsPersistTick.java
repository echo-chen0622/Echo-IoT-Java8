package org.thingsboard.server.actors.stats;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

public final class StatsPersistTick implements TbActorMsg {
    @Override
    public MsgType getMsgType() {
        return MsgType.STATS_PERSIST_TICK_MSG;
    }
}
