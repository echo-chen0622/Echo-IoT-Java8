package org.echoiot.rule.engine.debug;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.TbStopWatch;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.echoiot.common.util.DonAsynchron.withCallback;
import static org.echoiot.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generator",
        configClazz = TbMsgGeneratorNodeConfiguration.class,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    private static final String TB_MSG_GENERATOR_NODE_MSG = "TbMsgGeneratorNodeMsg";

    private TbMsgGeneratorNodeConfiguration config;
    @Nullable
    private ScriptEngine scriptEngine;
    private long delay;
    private long lastScheduledTs;
    private int currentMsgCount;
    private EntityId originatorId;
    private UUID nextTickId;
    @Nullable
    private TbMsg prevMsg;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void init(@NotNull TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        log.trace("init generator with config {}", configuration);
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        this.currentMsgCount = 0;
        if (!StringUtils.isEmpty(config.getOriginatorId())) {
            originatorId = EntityIdFactory.getByTypeAndUuid(config.getOriginatorType(), config.getOriginatorId());
            ctx.checkTenantEntity(originatorId);
        } else {
            originatorId = ctx.getSelfId();
        }
        updateGeneratorState(ctx);
    }

    @Override
    public void onPartitionChangeMsg(@NotNull TbContext ctx, PartitionChangeMsg msg) {
        log.trace("onPartitionChangeMsg, PartitionChangeMsg {}, config {}", msg, config);
        updateGeneratorState(ctx);
    }

    private void updateGeneratorState(@NotNull TbContext ctx) {
        log.trace("updateGeneratorState, config {}", config);
        if (ctx.isLocalEntity(originatorId)) {
            if (initialized.compareAndSet(false, true)) {
                this.scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                        ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript(), "prevMsg", "prevMetadata", "prevMsgType");
                scheduleTickMsg(ctx);
            }
        } else if (initialized.compareAndSet(true, false)) {
            destroy();
        }
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        log.trace("onMsg, config {}, msg {}", config, msg);
        if (initialized.get() && msg.getType().equals(TB_MSG_GENERATOR_NODE_MSG) && msg.getId().equals(nextTickId)) {
            @NotNull TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.enqueueForTellNext(m, SUCCESS);
                            scheduleTickMsg(ctx);
                            currentMsgCount++;
                        }
                    },
                    t -> {
                        log.trace("onMsg onFailure callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg, t);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.tellFailure(msg, t);
                            scheduleTickMsg(ctx);
                            currentMsgCount++;
                        }
                    });
        }
    }

    private void scheduleTickMsg(@NotNull TbContext ctx) {
        log.trace("scheduleTickMsg, config {}", config);
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_GENERATOR_NODE_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    @NotNull
    private ListenableFuture<TbMsg> generate(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(null, "", originatorId, msg.getCustomerId(), new TbMsgMetaData(), "{}");
        }
        if (initialized.get()) {
            ctx.logJsEvalRequest();
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                log.trace("generate process response, generated {}, config {}", generated, config);
                ctx.logJsEvalResponse();
                prevMsg = ctx.newMsg(null, generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

    }

    @Override
    public void destroy() {
        log.trace("destroy, config {}", config);
        prevMsg = null;
        if (scriptEngine != null) {
            scriptEngine.destroy();
            scriptEngine = null;
        }
    }
}
