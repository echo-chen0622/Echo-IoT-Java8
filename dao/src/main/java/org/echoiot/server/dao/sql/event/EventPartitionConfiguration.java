package org.echoiot.server.dao.sql.event;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.event.EventType;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class EventPartitionConfiguration {

    @Getter
    @Value("${sql.events.partition_size:168}")
    private int regularPartitionSizeInHours;
    @Getter
    @Value("${sql.events.debug_partition_size:1}")
    private int debugPartitionSizeInHours;

    private long regularPartitionSizeInMs;
    private long debugPartitionSizeInMs;

    @PostConstruct
    public void init() {
        regularPartitionSizeInMs = TimeUnit.HOURS.toMillis(regularPartitionSizeInHours);
        debugPartitionSizeInMs = TimeUnit.HOURS.toMillis(debugPartitionSizeInHours);
    }

    public long getPartitionSizeInMs(@NotNull EventType eventType) {
        return eventType.isDebug() ? debugPartitionSizeInMs : regularPartitionSizeInMs;
    }
}
