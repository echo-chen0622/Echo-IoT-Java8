package org.echoiot.rule.engine.flow;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

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
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRuleChainInputNodeConfiguration.class);
        this.ruleChainId = new RuleChainId(UUID.fromString(config.getRuleChainId()));
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        ctx.input(msg, ruleChainId);
    }

}
