package org.echoiot.common.util;

import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

@ToString
public class EchoiotForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    @NotNull
    private final String namePrefix;
    private final AtomicLong threadNumber = new AtomicLong(1);

    public EchoiotForkJoinWorkerThreadFactory(@NonNull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @NotNull
    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setContextClassLoader(this.getClass().getClassLoader());
        thread.setName(namePrefix +"-"+thread.getPoolIndex()+"-"+threadNumber.getAndIncrement());
        return thread;
    }
}
