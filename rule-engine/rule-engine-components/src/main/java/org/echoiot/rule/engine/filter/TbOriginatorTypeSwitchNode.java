package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.EmptyNodeConfiguration;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "entity type switch",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {"Device", "Asset", "Alarm", "Entity View", "Tenant", "Customer", "User", "Dashboard", "Rule chain", "Rule node"},
        nodeDescription = "Route incoming messages by Message Originator Type",
        nodeDetails = "Routes messages to chain according to the entity type ('Device', 'Asset', etc.).",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbOriginatorTypeSwitchNode extends TbAbstractTypeSwitchNode {

    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException {
        String relationType;
        EntityType originatorType = originator.getEntityType();
        switch (originatorType) {
            case TENANT:
                relationType = "Tenant";
                break;
            case CUSTOMER:
                relationType = "Customer";
                break;
            case USER:
                relationType = "User";
                break;
            case DASHBOARD:
                relationType = "Dashboard";
                break;
            case ASSET:
                relationType = "Asset";
                break;
            case DEVICE:
                relationType = "Device";
                break;
            case ENTITY_VIEW:
                relationType = "Entity View";
                break;
            case EDGE:
                relationType = "Edge";
                break;
            case RULE_CHAIN:
                relationType = "Rule chain";
                break;
            case RULE_NODE:
                relationType = "Rule node";
                break;
            case ALARM:
                relationType = "Alarm";
                break;
            default:
                throw new TbNodeException("Unsupported originator type: " + originatorType);
        }
        return relationType;
    }

}
