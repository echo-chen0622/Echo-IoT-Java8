package org.echoiot.server.queue.scheduler;

import org.echoiot.common.util.EchoiotThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
public class DefaultSchedulerComponent implements SchedulerComponent{

    protected ScheduledExecutorService schedulerExecutor;

    @PostConstruct
    public void init(){
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(EchoiotThreadFactory.forName("queue-scheduler"));
    }

    @PreDestroy
    public void destroy() {
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
    }

    @NotNull
    public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return schedulerExecutor.schedule(command, delay, unit);
    }

    @NotNull
    public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        return schedulerExecutor.schedule(callable, delay, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        return schedulerExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit) {
        return schedulerExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
