package org.echoiot.server.actors.ruleChain;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.actors.*;
import org.echoiot.server.actors.service.ComponentActor;
import org.echoiot.server.actors.service.ContextBasedCreator;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RuleNodeActor extends ComponentActor<RuleNodeId, RuleNodeActorMessageProcessor> {

    private final String ruleChainName;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;

    private RuleNodeActor(ActorSystemContext systemContext, TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, RuleNodeId ruleNodeId) {
        super(systemContext, tenantId, ruleNodeId);
        this.ruleChainName = ruleChainName;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
    }

    @NotNull
    @Override
    protected RuleNodeActorMessageProcessor createProcessor(@NotNull TbActorCtx ctx) {
        return new RuleNodeActorMessageProcessor(tenantId, this.ruleChainName, ruleNodeId, systemContext, ctx.getParentRef(), ctx);
    }

    @Override
    protected boolean doProcess(@NotNull TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
            case RULE_NODE_UPDATED_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_MSG:
                onRuleChainToRuleNodeMsg((RuleChainToRuleNodeMsg) msg);
                break;
            case RULE_TO_SELF_MSG:
                onRuleNodeToSelfMsg((RuleNodeToSelfMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            case PARTITION_CHANGE_MSG:
                onClusterEventMsg((PartitionChangeMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onRuleNodeToSelfMsg(@NotNull RuleNodeToSelfMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule msg: {}", ruleChainId, id, processor.getComponentName(), msg.getMsg());
        }
        try {
            processor.onRuleToSelfMsg(msg);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    private void onRuleChainToRuleNodeMsg(@NotNull RuleChainToRuleNodeMsg envelope) {
        TbMsg msg = envelope.getMsg();
        if (!msg.isValid()) {
            if (log.isTraceEnabled()) {
                log.trace("Skip processing of message: {} because it is no longer valid!", msg);
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule engine msg: {}", ruleChainId, id, processor.getComponentName(), msg);
        }
        try {
            processor.onRuleChainToRuleNodeMsg(envelope);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    public static class ActorCreator extends ContextBasedCreator {

        private final TenantId tenantId;
        private final RuleChainId ruleChainId;
        private final String ruleChainName;
        private final RuleNodeId ruleNodeId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, RuleNodeId ruleNodeId) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChainId = ruleChainId;
            this.ruleChainName = ruleChainName;
            this.ruleNodeId = ruleNodeId;

        }

        @NotNull
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(ruleNodeId);
        }

        @NotNull
        @Override
        public TbActor createActor() {
            return new RuleNodeActor(context, tenantId, ruleChainId, ruleChainName, ruleNodeId);
        }
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleNodeErrorPersistFrequency();
    }

}
