package org.echoiot.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.echoiot.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.concurrent.TimeUnit;


@Slf4j
@Repository
public class SqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    @Resource
    private EventPartitionConfiguration partitionConfiguration;
    @Resource
    private SqlPartitioningRepository partitioningRepository;

    @Override
    public void cleanupEvents(long eventExpTime, boolean debug) {
        for (@NotNull EventType eventType : EventType.values()) {
            if (eventType.isDebug() == debug) {
                cleanupEvents(eventType, eventExpTime);
            }
        }
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        regularEventTs = Math.max(regularEventTs, 1480982400000L);
        debugEventTs = Math.max(debugEventTs, 1480982400000L);

        callMigrateFunctionByPartitions("regular", "migrate_regular_events", regularEventTs, partitionConfiguration.getRegularPartitionSizeInHours());
        callMigrateFunctionByPartitions("debug", "migrate_debug_events", debugEventTs, partitionConfiguration.getDebugPartitionSizeInHours());

        try {
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_regular_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_debug_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP TABLE IF EXISTS event");
        } catch (DataAccessException e) {
            log.error("Error occurred during drop of the `events` table", e);
            throw e;
        }
    }

    private void callMigrateFunctionByPartitions(String logTag, String functionName, long startTs, int partitionSizeInHours) {
        long currentTs = System.currentTimeMillis();
        var regularPartitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTs - startTs) / regularPartitionStepInMs;
        if (numberOfPartitions > 1000) {
            log.error("Please adjust your {} events partitioning configuration. " +
                            "Configuration with partition size of {} hours and corresponding TTL will use {} (>1000) partitions which is not recommended!",
                    logTag, partitionSizeInHours, numberOfPartitions);
            throw new RuntimeException("Please adjust your " + logTag + " events partitioning configuration. " +
                    "Configuration with partition size of " + partitionSizeInHours + " hours and corresponding TTL will use " +
                    +numberOfPartitions + " (>1000) partitions which is not recommended!");
        }
        while (startTs < currentTs) {
            var endTs = startTs + regularPartitionStepInMs;
            log.info("Migrate {} events for time period: [{},{}]", logTag, startTs, endTs);
            callMigrateFunction(functionName, startTs, startTs + regularPartitionStepInMs, partitionSizeInHours);
            startTs = endTs;
        }
        log.info("Migrate {} events done.", logTag);
    }

    private void callMigrateFunction(String functionName, long startTs, long endTs, int partitionSizeInHours) {
        try {
            jdbcTemplate.update("CALL " + functionName + "(?, ?, ?)", startTs, endTs, partitionSizeInHours);
        } catch (DataAccessException e) {
            if (e.getMessage() == null || !e.getMessage().contains("relation \"event\" does not exist")) {
                log.error("[{}] SQLException occurred during execution of {} with parameters {} and {}", functionName, startTs, partitionSizeInHours, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanupEvents(@NotNull EventType eventType, long eventExpTime) {
        partitioningRepository.dropPartitionsBefore(eventType.getTable(), eventExpTime, partitionConfiguration.getPartitionSizeInMs(eventType));
    }

}
