package org.echoiot.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.msg.TbActorStopReason;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbRuleEngineActorMsg;
import org.echoiot.server.common.msg.aware.RuleChainAwareMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;

@EqualsAndHashCode(callSuper = true)
@ToString
public abstract class TbToRuleChainActorMsg extends TbRuleEngineActorMsg implements RuleChainAwareMsg {

    @Getter
    private final RuleChainId target;

    public TbToRuleChainActorMsg(TbMsg msg, RuleChainId target) {
        super(msg);
        this.target = target;
    }

    @Override
    public RuleChainId getRuleChainId() {
        return target;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? String.format("Rule chain [%s] stopped", target.getId()) : String.format("Failed to initialize rule chain [%s]!", target.getId());
        msg.getCallback().onFailure(new RuleEngineException(message));
    }
}
