package org.echoiot.server.actors.ruleChain;

import org.echoiot.server.actors.*;
import org.echoiot.server.actors.service.ComponentActor;
import org.echoiot.server.actors.service.ContextBasedCreator;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;
import org.echoiot.server.common.msg.queue.QueueToRuleEngineMsg;
import org.jetbrains.annotations.NotNull;

public class RuleChainActor extends ComponentActor<RuleChainId, RuleChainActorMessageProcessor> {

    @NotNull
    private final RuleChain ruleChain;

    private RuleChainActor(ActorSystemContext systemContext, TenantId tenantId, @NotNull RuleChain ruleChain) {
        super(systemContext, tenantId, ruleChain.getId());
        this.ruleChain = ruleChain;
    }

    @NotNull
    @Override
    protected RuleChainActorMessageProcessor createProcessor(@NotNull TbActorCtx ctx) {
        return new RuleChainActorMessageProcessor(tenantId, ruleChain, systemContext,
                ctx.getParentRef(), ctx);
    }

    @Override
    protected boolean doProcess(@NotNull TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                processor.onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case RULE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                processor.onTellNext((RuleNodeToRuleChainTellNextMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                processor.onRuleChainToRuleChainMsg((RuleChainToRuleChainMsg) msg);
                break;
            case RULE_CHAIN_INPUT_MSG:
                processor.onRuleChainInputMsg((RuleChainInputMsg) msg);
                break;
            case RULE_CHAIN_OUTPUT_MSG:
                processor.onRuleChainOutputMsg((RuleChainOutputMsg) msg);
                break;
            case PARTITION_CHANGE_MSG:
                processor.onPartitionChangeMsg((PartitionChangeMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            default:
                return false;
        }
        return true;
    }

    public static class ActorCreator extends ContextBasedCreator {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final RuleChain ruleChain;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChain ruleChain) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChain = ruleChain;
        }

        @NotNull
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(ruleChain.getId());
        }

        @NotNull
        @Override
        public TbActor createActor() {
            return new RuleChainActor(context, tenantId, ruleChain);
        }
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleChainErrorPersistFrequency();
    }

}
