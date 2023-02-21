package org.echoiot.server.actors;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
@AllArgsConstructor
public class ActorTestCtx {

    private volatile CountDownLatch latch;
    @NotNull
    private final AtomicInteger invocationCount;
    private final int expectedInvocationCount;
    @NotNull
    private final AtomicLong actual;

    public void clear() {
        latch = new CountDownLatch(1);
        invocationCount.set(0);
        actual.set(0L);
    }
}
