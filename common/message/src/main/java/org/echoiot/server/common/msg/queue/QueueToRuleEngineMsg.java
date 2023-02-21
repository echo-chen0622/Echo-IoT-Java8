package org.echoiot.server.common.msg.queue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorStopReason;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbRuleEngineActorMsg;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by Echo on 15.03.18.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class QueueToRuleEngineMsg extends TbRuleEngineActorMsg {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final Set<String> relationTypes;
    @Getter
    private final String failureMessage;

    public QueueToRuleEngineMsg(TenantId tenantId, TbMsg tbMsg, Set<String> relationTypes, String failureMessage) {
        super(tbMsg);
        this.tenantId = tenantId;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.QUEUE_TO_RULE_ENGINE_MSG;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message;
        if (msg.getRuleChainId() != null) {
            message = reason == TbActorStopReason.STOPPED ?
                    String.format("Rule chain [%s] stopped", msg.getRuleChainId().getId()) :
                    String.format("Failed to initialize rule chain [%s]!", msg.getRuleChainId().getId());
        } else {
            message = reason == TbActorStopReason.STOPPED ? "Rule chain stopped" : "Failed to initialize rule chain!";
        }
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    public boolean isTellNext() {
        return relationTypes != null && !relationTypes.isEmpty();
    }

}
