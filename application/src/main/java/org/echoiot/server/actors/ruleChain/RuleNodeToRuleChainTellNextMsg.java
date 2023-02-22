package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorStopReason;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbRuleEngineActorMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by Echo on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
class RuleNodeToRuleChainTellNextMsg extends TbRuleEngineActorMsg implements Serializable {

    private static final long serialVersionUID = 4577026446412871820L;
    @Getter
    private final RuleChainId ruleChainId;
    @Getter
    private final RuleNodeId originator;
    @Getter
    private final Set<String> relationTypes;
    @Getter
    private final String failureMessage;

    public RuleNodeToRuleChainTellNextMsg(RuleChainId ruleChainId, RuleNodeId originator, Set<String> relationTypes, TbMsg tbMsg, String failureMessage) {
        super(tbMsg);
        this.ruleChainId = ruleChainId;
        this.originator = originator;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? String.format("Rule chain [%s] stopped", ruleChainId.getId()) : String.format("Failed to initialize rule chain [%s]!", ruleChainId.getId());
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_RULE_CHAIN_TELL_NEXT_MSG;
    }

}
