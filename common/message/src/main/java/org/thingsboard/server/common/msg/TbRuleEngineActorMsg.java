package org.thingsboard.server.common.msg;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public abstract class TbRuleEngineActorMsg implements TbActorMsg {

    @Getter
    protected final TbMsg msg;

    public TbRuleEngineActorMsg(TbMsg msg) {
        this.msg = msg;
    }
}
