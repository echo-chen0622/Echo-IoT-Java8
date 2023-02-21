package org.echoiot.server.dao.sql;

import org.echoiot.common.util.EchoiotThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledLogExecutorComponent {

    private ScheduledExecutorService schedulerLogExecutor;

    @PostConstruct
    public void init() {
        schedulerLogExecutor = Executors.newSingleThreadScheduledExecutor(EchoiotThreadFactory.forName("sql-log"));
    }

    @PreDestroy
    public void stop() {
        if (schedulerLogExecutor != null) {
            schedulerLogExecutor.shutdownNow();
        }
    }

    public void scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        schedulerLogExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
