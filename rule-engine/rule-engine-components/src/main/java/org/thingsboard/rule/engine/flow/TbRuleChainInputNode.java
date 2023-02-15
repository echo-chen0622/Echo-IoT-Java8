package org.thingsboard.rule.engine.flow;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.FLOW,
        name = "rule chain",
        configClazz = TbRuleChainInputNodeConfiguration.class,
        nodeDescription = "transfers the message to another rule chain",
        nodeDetails = "Allows to nest the rule chain similar to single rule node. " +
                "The incoming message is forwarded to the input node of the specified target rule chain. " +
                "The target rule chain may produce multiple labeled outputs. " +
                "You may use the outputs to forward the results of processing to other rule nodes.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFlowNodeRuleChainInputConfig",
        relationTypes = {},
        ruleChainNode = true,
        customRelations = true
)
public class TbRuleChainInputNode implements TbNode {

    private TbRuleChainInputNodeConfiguration config;
    private RuleChainId ruleChainId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRuleChainInputNodeConfiguration.class);
        this.ruleChainId = new RuleChainId(UUID.fromString(config.getRuleChainId()));
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.input(msg, ruleChainId);
    }

}
