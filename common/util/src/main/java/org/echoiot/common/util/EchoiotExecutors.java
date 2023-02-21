package org.echoiot.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class EchoiotExecutors {

    /**
     * Method forked from ExecutorService to provide thread poll name
     *
     * Creates a thread pool that maintains enough threads to support
     * the given parallelism level, and may use multiple queues to
     * reduce contention. The parallelism level corresponds to the
     * maximum number of threads actively engaged in, or available to
     * engage in, task processing. The actual number of threads may
     * grow and shrink dynamically. A work-stealing pool makes no
     * guarantees about the order in which submitted tasks are
     * executed.
     *
     * @param parallelism the targeted parallelism level
     * @param namePrefix used to define thread name
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code parallelism <= 0}
     * @since 1.8
     */
    @NotNull
    public static ExecutorService newWorkStealingPool(int parallelism, @NotNull String namePrefix) {
        return new ForkJoinPool(parallelism,
                new EchoiotForkJoinWorkerThreadFactory(namePrefix),
                null, true);
    }

    @NotNull
    public static ExecutorService newWorkStealingPool(int parallelism, @NotNull Class clazz) {
        return newWorkStealingPool(parallelism, clazz.getSimpleName());
    }

}
