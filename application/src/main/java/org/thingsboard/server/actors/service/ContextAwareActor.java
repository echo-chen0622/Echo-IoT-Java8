package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.common.msg.TbActorMsg;

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

    protected ProcessFailureStrategy doProcessFailure(Throwable t) {
        if (t instanceof Error) {
            return ProcessFailureStrategy.stop();
        } else {
            return ProcessFailureStrategy.resume();
        }
    }
}
