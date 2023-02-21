package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
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
final class RuleChainToRuleNodeMsg extends TbToRuleNodeActorMsg {

    @Getter
    private final String fromRelationType;

    public RuleChainToRuleNodeMsg(TbContext ctx, TbMsg tbMsg, String fromRelationType) {
        super(ctx, tbMsg);
        this.fromRelationType = fromRelationType;
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_MSG;
    }
}
