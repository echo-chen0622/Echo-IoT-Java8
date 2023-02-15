package org.thingsboard.server.dao.sql;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

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
        schedulerLogExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("sql-log"));
    }

    @PreDestroy
    public void stop() {
        if (schedulerLogExecutor != null) {
            schedulerLogExecutor.shutdownNow();
        }
    }

    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        schedulerLogExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
