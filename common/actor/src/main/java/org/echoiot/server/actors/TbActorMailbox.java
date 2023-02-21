package org.echoiot.server.actors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.TbActorStopReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
@Data
public final class TbActorMailbox implements TbActorCtx {
    private static final boolean HIGH_PRIORITY = true;
    private static final boolean NORMAL_PRIORITY = false;

    private static final boolean FREE = false;
    private static final boolean BUSY = true;

    private static final boolean NOT_READY = false;
    private static final boolean READY = true;

    @NotNull
    private final TbActorSystem system;
    @NotNull
    private final TbActorSystemSettings settings;
    @NotNull
    private final TbActorId selfId;
    @NotNull
    private final TbActorRef parentRef;
    @NotNull
    private final TbActor actor;
    @NotNull
    private final Dispatcher dispatcher;
    private final ConcurrentLinkedQueue<TbActorMsg> highPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TbActorMsg> normalPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean(FREE);
    private final AtomicBoolean ready = new AtomicBoolean(NOT_READY);
    private final AtomicBoolean destroyInProgress = new AtomicBoolean();
    @Nullable
    private volatile TbActorStopReason stopReason;

    public void initActor() {
        dispatcher.getExecutor().execute(() -> tryInit(1));
    }

    private void tryInit(int attempt) {
        try {
            log.debug("[{}] Trying to init actor, attempt: {}", selfId, attempt);
            if (!destroyInProgress.get()) {
                actor.init(this);
                if (!destroyInProgress.get()) {
                    ready.set(READY);
                    tryProcessQueue(false);
                }
            }
        } catch (Throwable t) {
            log.debug("[{}] Failed to init actor, attempt: {}", selfId, attempt, t);
            int attemptIdx = attempt + 1;
            InitFailureStrategy strategy = actor.onInitFailure(attempt, t);
            if (strategy.isStop() || (settings.getMaxActorInitAttempts() > 0 && attemptIdx > settings.getMaxActorInitAttempts())) {
                log.info("[{}] Failed to init actor, attempt {}, going to stop attempts.", selfId, attempt, t);
                stopReason = TbActorStopReason.INIT_FAILED;
                destroy();
            } else if (strategy.getRetryDelay() > 0) {
                log.info("[{}] Failed to init actor, attempt {}, going to retry in attempts in {}ms", selfId, attempt, strategy.getRetryDelay());
                log.debug("[{}] Error", selfId, t);
                system.getScheduler().schedule(() -> dispatcher.getExecutor().execute(() -> tryInit(attemptIdx)), strategy.getRetryDelay(), TimeUnit.MILLISECONDS);
            } else {
                log.info("[{}] Failed to init actor, attempt {}, going to retry immediately", selfId, attempt);
                log.debug("[{}] Error", selfId, t);
                dispatcher.getExecutor().execute(() -> tryInit(attemptIdx));
            }
        }
    }

    private void enqueue(@NotNull TbActorMsg msg, boolean highPriority) {
        if (!destroyInProgress.get()) {
            if (highPriority) {
                highPriorityMsgs.add(msg);
            } else {
                normalPriorityMsgs.add(msg);
            }
            tryProcessQueue(true);
        } else {
            if (highPriority && msg.getMsgType().equals(MsgType.RULE_NODE_UPDATED_MSG)) {
                synchronized (this) {
                    if (stopReason == TbActorStopReason.INIT_FAILED) {
                        destroyInProgress.set(false);
                        stopReason = null;
                        initActor();
                    } else {
                        msg.onTbActorStopped(stopReason);
                    }
                }
            } else {
                msg.onTbActorStopped(stopReason);
            }
        }
    }

    private void tryProcessQueue(boolean newMsg) {
        if (ready.get() == READY) {
            if (newMsg || !highPriorityMsgs.isEmpty() || !normalPriorityMsgs.isEmpty()) {
                if (busy.compareAndSet(FREE, BUSY)) {
                    dispatcher.getExecutor().execute(this::processMailbox);
                } else {
                    log.trace("[{}] MessageBox is busy, new msg: {}", selfId, newMsg);
                }
            } else {
                log.trace("[{}] MessageBox is empty, new msg: {}", selfId, newMsg);
            }
        } else {
            log.trace("[{}] MessageBox is not ready, new msg: {}", selfId, newMsg);
        }
    }

    private void processMailbox() {
        boolean noMoreElements = false;
        for (int i = 0; i < settings.getActorThroughput(); i++) {
            TbActorMsg msg = highPriorityMsgs.poll();
            if (msg == null) {
                msg = normalPriorityMsgs.poll();
            }
            if (msg != null) {
                try {
                    log.debug("[{}] Going to process message: {}", selfId, msg);
                    actor.process(msg);
                } catch (TbRuleNodeUpdateException updateException) {
                    stopReason = TbActorStopReason.INIT_FAILED;
                    destroy();
                } catch (Throwable t) {
                    log.debug("[{}] Failed to process message: {}", selfId, msg, t);
                    ProcessFailureStrategy strategy = actor.onProcessFailure(t);
                    if (strategy.isStop()) {
                        system.stop(selfId);
                    }
                }
            } else {
                noMoreElements = true;
                break;
            }
        }
        if (noMoreElements) {
            busy.set(FREE);
            dispatcher.getExecutor().execute(() -> tryProcessQueue(false));
        } else {
            dispatcher.getExecutor().execute(this::processMailbox);
        }
    }

    @Override
    public TbActorId getSelf() {
        return selfId;
    }

    @Override
    public void tell(TbActorId target, TbActorMsg actorMsg) {
        system.tell(target, actorMsg);
    }

    @Override
    public void broadcastToChildren(TbActorMsg msg) {
        system.broadcastToChildren(selfId, msg);
    }

    @Override
    public void broadcastToChildrenByType(TbActorMsg msg, @NotNull EntityType entityType) {
        broadcastToChildren(msg, actorId -> entityType.equals(actorId.getEntityType()));
    }

    @Override
    public void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter) {
        system.broadcastToChildren(selfId, childFilter, msg);
    }

    @Override
    public List<TbActorId> filterChildren(Predicate<TbActorId> childFilter) {
        return system.filterChildren(selfId, childFilter);
    }

    @Override
    public void stop(TbActorId target) {
        system.stop(target);
    }

    @Override
    public TbActorRef getOrCreateChildActor(TbActorId actorId, @NotNull Supplier<String> dispatcher, @NotNull Supplier<TbActorCreator> creator) {
        TbActorRef actorRef = system.getActor(actorId);
        if (actorRef == null) {
            return system.createChildActor(dispatcher.get(), creator.get(), selfId);
        } else {
            return actorRef;
        }
    }

    public void destroy() {
        if (stopReason == null) {
            stopReason = TbActorStopReason.STOPPED;
        }
        destroyInProgress.set(true);
        dispatcher.getExecutor().execute(() -> {
            try {
                ready.set(NOT_READY);
                actor.destroy();
                highPriorityMsgs.forEach(msg -> msg.onTbActorStopped(stopReason));
                normalPriorityMsgs.forEach(msg -> msg.onTbActorStopped(stopReason));
            } catch (Throwable t) {
                log.warn("[{}] Failed to destroy actor: {}", selfId, t);
            }
        });
    }

    @Override
    public TbActorId getActorId() {
        return selfId;
    }

    @Override
    public void tell(@NotNull TbActorMsg actorMsg) {
        enqueue(actorMsg, NORMAL_PRIORITY);
    }

    @Override
    public void tellWithHighPriority(@NotNull TbActorMsg actorMsg) {
        enqueue(actorMsg, HIGH_PRIORITY);
    }

}
