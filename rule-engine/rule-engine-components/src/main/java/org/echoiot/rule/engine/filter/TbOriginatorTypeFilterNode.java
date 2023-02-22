package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "entity type",
        configClazz = TbOriginatorTypeFilterNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Filter incoming messages by the type of message originator entity",
        nodeDetails = "Checks that the entity type of the incoming message originator matches one of the values specified in the filter.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeOriginatorTypeConfig")
public class TbOriginatorTypeFilterNode implements TbNode {

    TbOriginatorTypeFilterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbOriginatorTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        EntityType originatorType = msg.getOriginator().getEntityType();
        ctx.tellNext(msg, config.getOriginatorTypes().contains(originatorType) ? "True" : "False");
    }

}
