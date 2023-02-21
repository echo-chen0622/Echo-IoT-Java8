package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
final class RuleNodeToSelfMsg extends TbToRuleNodeActorMsg {

    public RuleNodeToSelfMsg(TbContext ctx, TbMsg tbMsg) {
        super(ctx, tbMsg);
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_SELF_MSG;
    }

}
