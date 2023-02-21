package org.echoiot.server.actors;

import lombok.Getter;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

public class IntTbActorMsg implements TbActorMsg {

    @Getter
    private final int value;

    public IntTbActorMsg(int value) {
        this.value = value;
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.QUEUE_TO_RULE_ENGINE_MSG;
    }
}
