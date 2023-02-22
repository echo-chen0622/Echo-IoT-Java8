package org.echoiot.rule.engine.delay;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.echoiot.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delay (deprecated)",
        configClazz = TbMsgDelayNodeConfiguration.class,
        nodeDescription = "Delays incoming message (deprecated)",
        nodeDetails = "Delays messages for a configurable period. " +
                "Please note, this node acknowledges the message from the current queue (message will be removed from queue). " +
                "Deprecated because the acknowledged message still stays in memory (to be delayed) and this " +
                "does not guarantee that message will be processed even if the \"retry failures and timeouts\" processing strategy will be chosen.",
        icon = "pause",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDelayConfig"
)
public class TbMsgDelayNode implements TbNode {

    private static final String TB_MSG_DELAY_NODE_MSG = "TbMsgDelayNodeMsg";

    private TbMsgDelayNodeConfiguration config;
    private Map<UUID, TbMsg> pendingMsgs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        this.pendingMsgs = new HashMap<>();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(TB_MSG_DELAY_NODE_MSG)) {
            TbMsg pendingMsg = pendingMsgs.remove(UUID.fromString(msg.getData()));
            if (pendingMsg != null) {
                ctx.enqueueForTellNext(pendingMsg, SUCCESS);
            }
        } else {
            if (pendingMsgs.size() < config.getMaxPendingMsgs()) {
                pendingMsgs.put(msg.getId(), msg);
                TbMsg tickMsg = ctx.newMsg(null, TB_MSG_DELAY_NODE_MSG, ctx.getSelfId(), msg.getCustomerId(), new TbMsgMetaData(), msg.getId().toString());
                ctx.tellSelf(tickMsg, getDelay(msg));
                ctx.ack(msg);
            } else {
                ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached!"));
            }
        }
    }

    private long getDelay(TbMsg msg) {
        int periodInSeconds;
        if (config.isUseMetadataPeriodInSecondsPatterns()) {
            if (isParsable(msg, config.getPeriodInSecondsPattern())) {
                periodInSeconds = Integer.parseInt(TbNodeUtils.processPattern(config.getPeriodInSecondsPattern(), msg));
            } else {
                throw new RuntimeException("Can't parse period in seconds from metadata using pattern: " + config.getPeriodInSecondsPattern());
            }
        } else {
            periodInSeconds = config.getPeriodInSeconds();
        }
        return TimeUnit.SECONDS.toMillis(periodInSeconds);
    }

    private boolean isParsable(TbMsg msg, String pattern) {
        return NumberUtils.isParsable(TbNodeUtils.processPattern(pattern, msg));
    }

    @Override
    public void destroy() {
        pendingMsgs.clear();
    }
}
