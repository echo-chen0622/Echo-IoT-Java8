package org.echoiot.rule.engine.telemetry;

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.echoiot.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.echoiot.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.echoiot.server.common.data.DataConstants.SCOPE;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save attributes",
        configClazz = TbMsgAttributesNodeConfiguration.class,
        nodeDescription = "Saves attributes data",
        nodeDetails = "Saves entity attributes based on configurable scope parameter. Expects messages with 'POST_ATTRIBUTES_REQUEST' message type. " +
                      "If upsert(update/insert) operation is completed successfully rule node will send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used. " +
                      "Additionally if checkbox <b>Send attributes updated notification</b> is set to true, rule node will put the \"Attributes Updated\" " +
                      "event for <b>SHARED_SCOPE</b> and <b>SERVER_SCOPE</b> attributes updates to the corresponding rule engine queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAttributesConfig",
        icon = "file_upload"
)
public class TbMsgAttributesNode implements TbNode {

    private TbMsgAttributesNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgAttributesNodeConfiguration.class);
        if (config.getNotifyDevice() == null) {
            config.setNotifyDevice(true);
        }
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        if (!msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        String src = msg.getData();
        @NotNull List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(src)));
        if (attributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        String scope = getScope(msg.getMetaData().getValue(SCOPE));
        boolean sendAttributesUpdateNotification = checkSendNotification(scope);
        ctx.getTelemetryService().saveAndNotify(
                ctx.getTenantId(),
                msg.getOriginator(),
                scope,
                attributes,
                checkNotifyDevice(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY)),
                sendAttributesUpdateNotification ?
                        new AttributesUpdateNodeCallback(ctx, msg, scope, attributes) :
                        new TelemetryNodeCallback(ctx, msg)
        );
    }

    private boolean checkSendNotification(String scope) {
        return config.isSendAttributesUpdatedNotification() && !CLIENT_SCOPE.equals(scope);
    }

    private boolean checkNotifyDevice(String notifyDeviceMdValue) {
        return config.getNotifyDevice() || StringUtils.isEmpty(notifyDeviceMdValue) || Boolean.parseBoolean(notifyDeviceMdValue);
    }

    private String getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return mdScopeValue;
        }
        return config.getScope();
    }

}
