package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbMsg;

/**
 * Created by Echo on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainInputMsg extends TbToRuleChainActorMsg {

    public RuleChainInputMsg(RuleChainId target, TbMsg tbMsg) {
        super(tbMsg, target);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_INPUT_MSG;
    }
}
