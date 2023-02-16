package org.echoiot.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.echoiot.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Slf4j
@Service
@ConditionalOnExpression("${sql.ttl.edge_events.enabled:true} && ${sql.ttl.edge_events.edge_event_ttl:0} > 0")
public class EdgeEventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}";

    @Value("${sql.ttl.edge_events.edge_events_ttl}")
    private long ttl;

    @Value("${sql.edge_events.partition_size:168}")
    private int partitionSizeInHours;

    @Value("${sql.ttl.edge_events.enabled:true}")
    private boolean ttlTaskExecutionEnabled;

    private final EdgeEventService edgeEventService;

    private final SqlPartitioningRepository partitioningRepository;

    public EdgeEventsCleanUpService(PartitionService partitionService, EdgeEventService edgeEventService, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.edgeEventService = edgeEventService;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        long edgeEventsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            edgeEventService.cleanupEvents(edgeEventsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(ModelConstants.EDGE_EVENT_COLUMN_FAMILY_NAME, edgeEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
