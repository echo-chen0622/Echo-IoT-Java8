package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

public interface TbActor {

    boolean process(TbActorMsg msg);

    TbActorRef getActorRef();

    default void init(TbActorCtx ctx) throws TbActorException {
    }

    default void destroy() throws TbActorException {
    }

    default InitFailureStrategy onInitFailure(int attempt, Throwable t) {
        return InitFailureStrategy.retryWithDelay(5000L * attempt);
    }

    default ProcessFailureStrategy onProcessFailure(Throwable t) {
        if (t instanceof Error) {
            return ProcessFailureStrategy.stop();
        } else {
            return ProcessFailureStrategy.resume();
        }
    }
}
