package org.echoiot.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.EmptyNodeConfiguration;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "split array msg",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Split array message into several msgs",
        nodeDetails = "Split the array fetched from the msg body. If the msg data is not a JSON array returns the "
                + "incoming message as outbound message with <code>Failure</code> chain, otherwise returns "
                + "inner objects of the extracted array as separate messages via <code>Success</code> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "content_copy",
        configDirective = "tbNodeEmptyConfig"
)
public class TbSplitArrayMsgNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        JsonNode jsonNode = JacksonUtil.toJsonNode(msg.getData());
        if (jsonNode.isArray()) {
            @NotNull ArrayNode data = (ArrayNode) jsonNode;
            if (data.isEmpty()) {
                ctx.ack(msg);
            } else if (data.size() == 1) {
                ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(data.get(0))));
            } else {
                @NotNull TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(data.size(), new TbMsgCallback() {
                    @Override
                    public void onSuccess() {
                        ctx.ack(msg);
                    }

                    @Override
                    public void onFailure(RuleEngineException e) {
                        ctx.tellFailure(msg, e);
                    }
                });
                data.forEach(msgNode -> ctx.enqueueForTellNext(TbMsg.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgNode)),
                        TbRelationTypes.SUCCESS, wrapper::onSuccess, wrapper::onFailure));
            }
        } else {
            ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Array!"));
        }
    }
}
