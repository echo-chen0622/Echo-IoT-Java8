package org.echoiot.rule.engine.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to cloud",
        configClazz = TbMsgPushToCloudNodeConfiguration.class,
        nodeDescription = "Pushes messages from edge to cloud",
        nodeDetails = "Push messages from edge to cloud. " +
                "This node used only on edge to push messages from edge to cloud. " +
                "Once message arrived into this node it’s going to be converted into cloud event and saved to the local database. " +
                "Node doesn't push messages directly to cloud, but stores event(s) in the cloud queue. " +
                "<br>Supports next originator types:" +
                "<br><code>DEVICE</code>" +
                "<br><code>ASSET</code>" +
                "<br><code>ENTITY_VIEW</code>" +
                "<br><code>DASHBOARD</code>" +
                "<br><code>TENANT</code>" +
                "<br><code>CUSTOMER</code>" +
                "<br><code>EDGE</code><br><br>" +
                "As well node supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save cloud event to database or unsupported originator type/message type arrived. " +
                "In case successful storage cloud event to database message will be routed via <b>Success</b> route.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePushToCloudConfig",
        icon = "cloud_upload",
        ruleChainTypes = RuleChainType.EDGE
)
public class TbMsgPushToCloudNode extends AbstractTbMsgPushNode<TbMsgPushToCloudNodeConfiguration, Object, Object> {

    // Implementation of this node is done on the Edge

    @Nullable
    @Override
    Object buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, Object eventType, JsonNode entityBody) {
        return null;
    }

    @Nullable
    @Override
    Object getEventTypeByEntityType(EntityType entityType) {
        return null;
    }

    @Nullable
    @Override
    Object getAlarmEventType() {
        return null;
    }

    @Nullable
    @Override
    String getIgnoredMessageSource() {
        return null;
    }

    @NotNull
    @Override
    protected Class<TbMsgPushToCloudNodeConfiguration> getConfigClazz() {
        return TbMsgPushToCloudNodeConfiguration.class;
    }

    @Override
    void processMsg(TbContext ctx, TbMsg msg) {
    }

}
