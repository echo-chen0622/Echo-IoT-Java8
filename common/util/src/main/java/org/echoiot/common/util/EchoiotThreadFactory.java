package org.echoiot.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copy of Executors.DefaultThreadFactory but with ability to set name of the pool
 */
public class EchoiotThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    @NotNull
    private final String namePrefix;

    @NotNull
    public static EchoiotThreadFactory forName(String name) {
        return new EchoiotThreadFactory(name);
    }

    private EchoiotThreadFactory(String name) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = name + "-" +
                poolNumber.getAndIncrement() +
                "-thread-";
    }

    @NotNull
    @Override
    public Thread newThread(Runnable r) {
        @NotNull Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                                       0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
