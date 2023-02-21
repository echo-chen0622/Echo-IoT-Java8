package org.echoiot.rule.engine.transaction;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.EmptyNodeConfiguration;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "synchronization end",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "This Node is now deprecated. Use \"Checkpoint\" instead.",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = ("tbNodeEmptyConfig")
)
@Deprecated
public class TbSynchronizationEndNode implements TbNode {

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        log.warn("Synchronization Start/End nodes are deprecated since TB 2.5. Use queue with submit strategy SEQUENTIAL_BY_ORIGINATOR instead.");
        ctx.tellSuccess(msg);
    }

}
