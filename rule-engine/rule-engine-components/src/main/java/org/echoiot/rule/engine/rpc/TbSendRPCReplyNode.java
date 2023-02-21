package org.echoiot.rule.engine.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call reply",
        configClazz = TbSendRpcReplyNodeConfiguration.class,
        nodeDescription = "Sends reply to RPC call from device",
        nodeDetails = "Expects messages with any message type. Will forward message body to the device.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRpcReplyConfig",
        icon = "call_merge"
)
public class TbSendRPCReplyNode implements TbNode {

    private TbSendRpcReplyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcReplyNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        String serviceIdStr = msg.getMetaData().getValue(config.getServiceIdMetaDataAttribute());
        String sessionIdStr = msg.getMetaData().getValue(config.getSessionIdMetaDataAttribute());
        String requestIdStr = msg.getMetaData().getValue(config.getRequestIdMetaDataAttribute());
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellFailure(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (StringUtils.isEmpty(requestIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Request id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(serviceIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Service id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(sessionIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Session id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(msg.getData())) {
            ctx.tellFailure(msg, new RuntimeException("Request body is empty!"));
        } else {
            if (StringUtils.isNotBlank(msg.getMetaData().getValue(DataConstants.EDGE_ID))) {
                saveRpcResponseToEdgeQueue(ctx, msg, serviceIdStr, sessionIdStr, requestIdStr);
            } else {
                ctx.getRpcService().sendRpcReplyToDevice(serviceIdStr, UUID.fromString(sessionIdStr), Integer.parseInt(requestIdStr), msg.getData());
                ctx.tellSuccess(msg);
            }
        }
    }

    private void saveRpcResponseToEdgeQueue(@NotNull TbContext ctx, @NotNull TbMsg msg, String serviceIdStr, String sessionIdStr, String requestIdStr) {
        EdgeId edgeId;
        DeviceId deviceId;
        try {
            edgeId = new EdgeId(UUID.fromString(msg.getMetaData().getValue(DataConstants.EDGE_ID)));
            deviceId = new DeviceId(UUID.fromString(msg.getMetaData().getValue(DataConstants.DEVICE_ID)));
        } catch (Exception e) {
            String errMsg = String.format("[%s] Failed to parse edgeId or deviceId from metadata %s!", ctx.getTenantId(), msg.getMetaData());
            ctx.tellFailure(msg, new RuntimeException(errMsg));
            return;
        }

        ObjectNode body = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        body.put("serviceId", serviceIdStr);
        body.put("sessionId", sessionIdStr);
        body.put("requestId", requestIdStr);
        body.put("response", msg.getData());
        @NotNull EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(ctx.getTenantId(), edgeId, EdgeEventType.DEVICE,
                                                                    EdgeEventActionType.RPC_CALL, deviceId, JacksonUtil.OBJECT_MAPPER.valueToTree(body));
        ListenableFuture<Void> future = ctx.getEdgeEventService().saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
                ctx.tellSuccess(msg);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }
}
