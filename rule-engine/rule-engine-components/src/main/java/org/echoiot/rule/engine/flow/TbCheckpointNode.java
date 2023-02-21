package org.echoiot.rule.engine.flow;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RuleNode(
        type = ComponentType.FLOW,
        name = "checkpoint",
        configClazz = TbCheckpointNodeConfiguration.class,
        nodeDescription = "transfers the message to another queue",
        nodeDetails = "After successful transfer incoming message is automatically acknowledged. Queue name is configurable.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCheckPointConfig"
)
public class TbCheckpointNode implements TbNode {

    private String queueName;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        TbCheckpointNodeConfiguration config = TbNodeUtils.convert(configuration, TbCheckpointNodeConfiguration.class);
        this.queueName = config.getQueueName();
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        ctx.enqueueForTellNext(msg, queueName, TbRelationTypes.SUCCESS, () -> ctx.ack(msg), error -> ctx.tellFailure(msg, error));
    }

}
