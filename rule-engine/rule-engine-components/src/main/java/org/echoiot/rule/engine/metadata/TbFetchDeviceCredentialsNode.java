package org.echoiot.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "fetch device credentials",
        configClazz = TbFetchDeviceCredentialsNodeConfiguration.class,
        nodeDescription = "Fetch device credentials for message originator",
        nodeDetails = "Adds <b>credentialsType</b> and <b>credentials</b> properties to the message metadata if the " +
                "configuration parameter <b>fetchToMetadata</b> is set to <code>true</code>, otherwise, adds properties " +
                "to the message data. If originator type is not <b>DEVICE</b> or rule node failed to get device credentials " +
                "- send Message via <code>Failure</code> chain, otherwise <code>Success</code> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeFetchDeviceCredentialsConfig")
public class TbFetchDeviceCredentialsNode implements TbNode {

    private static final String CREDENTIALS = "credentials";
    private static final String CREDENTIALS_TYPE = "credentialsType";

    TbFetchDeviceCredentialsNodeConfiguration config;
    boolean fetchToMetadata;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbFetchDeviceCredentialsNodeConfiguration.class);
        this.fetchToMetadata = config.isFetchToMetadata();
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        EntityId originator = msg.getOriginator();
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type: " + originator.getEntityType() + "!"));
            return;
        }
        @NotNull DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
        DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(ctx.getTenantId(), deviceId);
        if (deviceCredentials == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to get Device Credentials for device: " + deviceId + "!"));
            return;
        }

        TbMsg transformedMsg;
        DeviceCredentialsType credentialsType = deviceCredentials.getCredentialsType();
        @Nullable JsonNode credentialsInfo = ctx.getDeviceCredentialsService().toCredentialsInfo(deviceCredentials);
        if (fetchToMetadata) {
            TbMsgMetaData metaData = msg.getMetaData();
            metaData.putValue(CREDENTIALS_TYPE, credentialsType.name());
            if (credentialsType.equals(DeviceCredentialsType.ACCESS_TOKEN) || credentialsType.equals(DeviceCredentialsType.X509_CERTIFICATE)) {
                metaData.putValue(CREDENTIALS, credentialsInfo.asText());
            } else {
                metaData.putValue(CREDENTIALS, JacksonUtil.toString(credentialsInfo));
            }
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, metaData, msg.getData());
        } else {
            ObjectNode data = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            data.put(CREDENTIALS_TYPE, credentialsType.name());
            data.set(CREDENTIALS, credentialsInfo);
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, msg.getMetaData(), JacksonUtil.toString(data));
        }
        ctx.tellSuccess(transformedMsg);
    }
}
