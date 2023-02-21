package org.echoiot.rule.engine.action;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.echoiot.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "message count",
        configClazz = TbMsgCountNodeConfiguration.class,
        nodeDescription = "Count incoming messages",
        nodeDetails = "Count incoming messages for specified interval and produces POST_TELEMETRY_REQUEST msg with messages count",
        icon = "functions",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgCountConfig"
)
public class TbMsgCountNode implements TbNode {

    private static final String TB_MSG_COUNT_NODE_MSG = "TbMsgCountNodeMsg";

    @NotNull
    private AtomicLong messagesProcessed = new AtomicLong(0);
    private final Gson gson = new Gson();
    private UUID nextTickId;
    private long delay;
    private String telemetryPrefix;
    private long lastScheduledTs;

    @Override
    public void init(@NotNull TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgCountNodeConfiguration config = TbNodeUtils.convert(configuration, TbMsgCountNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.telemetryPrefix = config.getTelemetryPrefix();
        scheduleTickMsg(ctx, null);

    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        if (msg.getType().equals(TB_MSG_COUNT_NODE_MSG) && msg.getId().equals(nextTickId)) {
            @NotNull JsonObject telemetryJson = new JsonObject();
            telemetryJson.addProperty(this.telemetryPrefix + "_" + ctx.getServiceId(), messagesProcessed.longValue());

            messagesProcessed = new AtomicLong(0);

            @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("delta", Long.toString(System.currentTimeMillis() - lastScheduledTs + delay));

            @NotNull TbMsg tbMsg = TbMsg.newMsg(msg.getQueueName(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), ctx.getTenantId(), msg.getCustomerId(), metaData, gson.toJson(telemetryJson));
            ctx.enqueueForTellNext(tbMsg, SUCCESS);
            scheduleTickMsg(ctx, tbMsg);
        } else {
            messagesProcessed.incrementAndGet();
            ctx.ack(msg);
        }
    }

    private void scheduleTickMsg(@NotNull TbContext ctx, @Nullable TbMsg msg) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_COUNT_NODE_MSG, ctx.getSelfId(), msg != null ? msg.getCustomerId() : null, new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

}
