package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainToRuleChainMsg extends TbToRuleChainActorMsg  {

    @Getter
    private final RuleChainId source;
    @Getter
    private final String fromRelationType;

    public RuleChainToRuleChainMsg(RuleChainId target, RuleChainId source, TbMsg tbMsg, String fromRelationType) {
        super(tbMsg, target);
        this.source = source;
        this.fromRelationType = fromRelationType;
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_CHAIN_MSG;
    }
}
