package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SlowCreateActor extends TestRootActor {

    public static final int TIMEOUT_AWAIT_MAX_MS = 5000;

    public SlowCreateActor(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
        super(actorId, testCtx);
        try {
            initLatch.await(TIMEOUT_AWAIT_MAX_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testCtx.getInvocationCount().incrementAndGet();
    }

    public static class SlowCreateActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;
        private final CountDownLatch initLatch;

        public SlowCreateActorCreator(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
            this.actorId = actorId;
            this.testCtx = testCtx;
            this.initLatch = initLatch;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return new SlowCreateActor(actorId, testCtx, initLatch);
        }
    }
}
