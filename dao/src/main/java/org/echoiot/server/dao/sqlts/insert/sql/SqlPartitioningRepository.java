package org.echoiot.server.dao.sqlts.insert.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.echoiot.server.dao.timeseries.SqlPartition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
@Slf4j
public class SqlPartitioningRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String SELECT_PARTITIONS_STMT = "SELECT tablename from pg_tables WHERE schemaname = 'public' and tablename like concat(?, '_%')";

    private static final int PSQL_VERSION_14 = 140000;
    @Nullable
    private volatile Integer currentServerVersion;

    private final Map<String, Map<Long, SqlPartition>> tablesPartitions = new ConcurrentHashMap<>();
    private final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void save(@NotNull SqlPartition partition) {
        jdbcTemplate.execute(partition.getQuery());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // executing non-transactionally, so that parent transaction is not aborted on partition save error
    public void createPartitionIfNotExists(String table, long entityTs, long partitionDurationMs) {
        long partitionStartTs = calculatePartitionStartTime(entityTs, partitionDurationMs);
        @NotNull Map<Long, SqlPartition> partitions = tablesPartitions.computeIfAbsent(table, t -> new ConcurrentHashMap<>());
        if (!partitions.containsKey(partitionStartTs)) {
            @NotNull SqlPartition partition = new SqlPartition(table, partitionStartTs, partitionStartTs + partitionDurationMs, Long.toString(partitionStartTs));
            partitionCreationLock.lock();
            try {
                if (partitions.containsKey(partitionStartTs)) return;
                log.info("Saving partition {}-{} for table {}", partition.getStart(), partition.getEnd(), table);
                save(partition);
                log.trace("Adding partition to map: {}", partition);
                partitions.put(partition.getStart(), partition);
            } catch (Exception e) {
                @NotNull String error = ExceptionUtils.getRootCauseMessage(e);
                if (StringUtils.containsAny(error, "would overlap partition", "already exists")) {
                    partitions.put(partition.getStart(), partition);
                    log.debug("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                } else {
                    log.warn("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    public void dropPartitionsBefore(@NotNull String table, long ts, long partitionDurationMs) {
        @NotNull List<Long> partitions = fetchPartitions(table);
        for (Long partitionStartTime : partitions) {
            long partitionEndTime = partitionStartTime + partitionDurationMs;
            if (partitionEndTime < ts) {
                log.info("[{}] Detaching expired partition: [{}-{}]", table, partitionStartTime, partitionEndTime);
                boolean success = detachAndDropPartition(table, partitionStartTime);
                if (success) {
                    log.info("[{}] Detached expired partition: {}", table, partitionStartTime);
                }
            } else {
                log.debug("[{}] Skipping valid partition: {}", table, partitionStartTime);
            }
        }
    }

    public void cleanupPartitionsCache(String table, long expTime, long partitionDurationMs) {
        Map<Long, SqlPartition> partitions = tablesPartitions.get(table);
        if (partitions == null) return;
        partitions.keySet().removeIf(startTime -> (startTime + partitionDurationMs) < expTime);
    }

    private boolean detachAndDropPartition(String table, long partitionTs) {
        Map<Long, SqlPartition> cachedPartitions = tablesPartitions.get(table);
        if (cachedPartitions != null) cachedPartitions.remove(partitionTs);

        @NotNull String tablePartition = table + "_" + partitionTs;
        @NotNull String detachPsqlStmtStr = "ALTER TABLE " + table + " DETACH PARTITION " + tablePartition;
        if (getCurrentServerVersion() >= PSQL_VERSION_14) {
            detachPsqlStmtStr += " CONCURRENTLY";
        }

        @NotNull String dropStmtStr = "DROP TABLE " + tablePartition;
        try {
            jdbcTemplate.execute(detachPsqlStmtStr);
            jdbcTemplate.execute(dropStmtStr);
            return true;
        } catch (DataAccessException e) {
            log.error("[{}] Error occurred trying to detach and drop the partition {} ", table, partitionTs, e);
        }
        return false;
    }

    @NotNull
    public List<Long> fetchPartitions(@NotNull String table) {
        @NotNull List<Long> partitions = new ArrayList<>();
        @NotNull List<String> partitionsTables = jdbcTemplate.queryForList(SELECT_PARTITIONS_STMT, String.class, table);
        for (@NotNull String partitionTableName : partitionsTables) {
            @NotNull String partitionTsStr = partitionTableName.substring(table.length() + 1);
            try {
                partitions.add(Long.parseLong(partitionTsStr));
            } catch (NumberFormatException nfe) {
                log.warn("Failed to parse table name: {}", partitionTableName);
            }
        }
        return partitions;
    }

    public long calculatePartitionStartTime(long ts, long partitionDuration) {
        return ts - (ts % partitionDuration);
    }

    private synchronized int getCurrentServerVersion() {
        if (currentServerVersion == null) {
            try {
                currentServerVersion = jdbcTemplate.queryForObject("SELECT current_setting('server_version_num')", Integer.class);
            } catch (Exception e) {
                log.warn("Error occurred during fetch of the server version", e);
            }
            if (currentServerVersion == null) {
                currentServerVersion = 0;
            }
        }
        return currentServerVersion;
    }

}
