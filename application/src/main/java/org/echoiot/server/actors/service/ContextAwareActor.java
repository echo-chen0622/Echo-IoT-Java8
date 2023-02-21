package org.echoiot.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.actors.AbstractTbActor;
import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.actors.ProcessFailureStrategy;
import org.jetbrains.annotations.NotNull;

@Slf4j
public abstract class ContextAwareActor extends AbstractTbActor {

    public static final int ENTITY_PACK_LIMIT = 1024;

    protected final ActorSystemContext systemContext;

    public ContextAwareActor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    @Override
    public boolean process(TbActorMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("Processing msg: {}", msg);
        }
        if (!doProcess(msg)) {
            log.warn("Unprocessed message: {}!", msg);
        }
        return false;
    }

    protected abstract boolean doProcess(TbActorMsg msg);

    @Override
    public ProcessFailureStrategy onProcessFailure(Throwable t) {
        log.debug("[{}] Processing failure: ", getActorRef().getActorId(), t);
        return doProcessFailure(t);
    }

    @NotNull
    protected ProcessFailureStrategy doProcessFailure(Throwable t) {
        if (t instanceof Error) {
            return ProcessFailureStrategy.stop();
        } else {
            return ProcessFailureStrategy.resume();
        }
    }
}
