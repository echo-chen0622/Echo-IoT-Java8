package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "message type",
        configClazz = TbMsgTypeFilterNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Filter incoming messages by Message Type",
        nodeDetails = "If incoming MessageType is expected - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeMessageTypeConfig")
public class TbMsgTypeFilterNode implements TbNode {

    TbMsgTypeFilterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        ctx.tellNext(msg, config.getMessageTypes().contains(msg.getType()) ? "True" : "False");
    }

}
