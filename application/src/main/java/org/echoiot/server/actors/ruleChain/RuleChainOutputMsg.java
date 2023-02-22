package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbMsg;

/**
 * Created by Echo on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainOutputMsg extends TbToRuleChainActorMsg {

    @Getter
    private final RuleNodeId targetRuleNodeId;

    @Getter
    private final String relationType;

    public RuleChainOutputMsg(RuleChainId target, RuleNodeId targetRuleNodeId, String relationType, TbMsg tbMsg) {
        super(tbMsg, target);
        this.targetRuleNodeId = targetRuleNodeId;
        this.relationType = relationType;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_OUTPUT_MSG;
    }
}
