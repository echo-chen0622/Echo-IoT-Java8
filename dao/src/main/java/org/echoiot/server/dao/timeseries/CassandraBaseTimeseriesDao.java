package org.echoiot.server.dao.timeseries;

import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.nosql.TbResultSet;
import org.echoiot.server.dao.nosql.TbResultSetFuture;
import org.echoiot.server.dao.sqlts.AggregationTimeseriesDao;
import org.echoiot.server.dao.util.NoSqlTsDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

/**
 * @author Andrew Shvayka
 */
@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
@NoSqlTsDao
public class CassandraBaseTimeseriesDao extends AbstractCassandraBaseTimeseriesDao implements TimeseriesDao, AggregationTimeseriesDao {

    protected static final int MIN_AGGREGATION_STEP_MS = 1000;
    public static final String ASC_ORDER = "ASC";
    public static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    protected static final List<Long> FIXED_PARTITION = List.of(0L);

    private CassandraTsPartitionsCache cassandraTsPartitionsCache;

    @Resource
    private Environment environment;

    @Getter
    @Value("${cassandra.query.ts_key_value_partitioning}")
    private String partitioning;

    @Getter
    @Value("${cassandra.query.use_ts_key_value_partitioning_on_read:true}")
    private boolean useTsKeyValuePartitioningOnRead;

    @Value("${cassandra.query.ts_key_value_partitions_max_cache_size:100000}")
    private long partitionsCacheSize;

    @Value("${cassandra.query.ts_key_value_ttl}")
    private long systemTtl;

    @Value("${cassandra.query.set_null_values_enabled}")
    private boolean setNullValuesEnabled;

    private NoSqlTsPartitionDate tsFormat;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement partitionInsertTtlStmt;
    private PreparedStatement[] saveStmts;
    private PreparedStatement[] saveTtlStmts;
    private PreparedStatement[] fetchStmtsAsc;
    private PreparedStatement[] fetchStmtsDesc;
    private PreparedStatement deleteStmt;
    private final Lock stmtCreationLock = new ReentrantLock();

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        super.startExecutor();
        if (!isInstall()) {
            getFetchStmt(Aggregation.NONE, DESC_ORDER);
        }
        @NotNull Optional<NoSqlTsPartitionDate> partition = NoSqlTsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
            if (!isFixedPartitioning() && partitionsCacheSize > 0) {
                cassandraTsPartitionsCache = new CassandraTsPartitionsCache(partitionsCacheSize);
            }
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @NotNull
    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, @NotNull EntityId entityId, @NotNull List<ReadTsKvQuery> queries) {
        @NotNull List<ListenableFuture<ReadTsKvQueryResult>> futures = queries.stream()
                                                                              .map(query -> findAllAsync(tenantId, entityId, query)).collect(Collectors.toList());
        return Futures.allAsList(futures);
    }

    @NotNull
    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, @NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry, long ttl) {
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        ttl = computeTtl(ttl);
        int dataPointDays = tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
        long partition = toPartitionTs(tsKvEntry.getTs());
        DataType type = tsKvEntry.getDataType();
        if (setNullValuesEnabled) {
            processSetNullValues(tenantId, entityId, tsKvEntry, ttl, futures, partition, type);
        }
        @NotNull BoundStatementBuilder stmtBuilder = new BoundStatementBuilder((ttl == 0 ? getSaveStmt(type) : getSaveTtlStmt(type)).bind());
        stmtBuilder.setString(0, entityId.getEntityType().name())
                .setUuid(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, partition)
                .setLong(4, tsKvEntry.getTs());
        addValue(tsKvEntry, stmtBuilder, 5);
        if (ttl > 0) {
            stmtBuilder.setInt(6, (int) ttl);
        }
        @NotNull BoundStatement stmt = stmtBuilder.build();
        futures.add(getFuture(executeAsyncWrite(tenantId, stmt), rs -> null));
        return Futures.transform(Futures.allAsList(futures), result -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, @NotNull EntityId entityId, long tsKvEntryTs, String key) {
        if (isFixedPartitioning()) {
            return Futures.immediateFuture(null);
        }
        // DO NOT apply custom TTL to partition, otherwise, short TTL will remove partition too early
        // partitions must remain in the DB forever or be removed only by systemTtl
        // removal of empty partition is too expensive (we need to scan all data keys for these partitions with ALLOW FILTERING)
        long ttl = computeTtl(0);
        long partition = toPartitionTs(tsKvEntryTs);
        if (cassandraTsPartitionsCache == null) {
            return doSavePartition(tenantId, entityId, key, ttl, partition);
        } else {
            @NotNull CassandraPartitionCacheKey partitionSearchKey = new CassandraPartitionCacheKey(entityId, key, partition);
            if (!cassandraTsPartitionsCache.has(partitionSearchKey)) {
                ListenableFuture<Integer> result = doSavePartition(tenantId, entityId, key, ttl, partition);
                Futures.addCallback(result, new CacheCallback<>(partitionSearchKey), MoreExecutors.directExecutor());
                return result;
            } else {
                return Futures.immediateFuture(0);
            }
        }
    }

    @NotNull
    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, @NotNull EntityId entityId, @NotNull DeleteTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());

        TbResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);

        @NotNull final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
        @NotNull final ListenableFuture<List<Long>> partitionsListFuture = Futures.transformAsync(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);

        Futures.addCallback(partitionsListFuture, new FutureCallback<List<Long>>() {
            @Override
            public void onSuccess(@Nullable List<Long> partitions) {
                @NotNull QueryCursor cursor = new QueryCursor(entityId.getEntityType().name(), entityId.getId(), query, partitions);
                deleteAsync(tenantId, cursor, resultFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to fetch partitions for interval {}-{}", entityId.getEntityType().name(), entityId.getId(), minPartition, maxPartition, t);
            }
        }, readResultsProcessingExecutor);
        return resultFuture;
    }

    @NotNull
    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, @NotNull EntityId entityId, @NotNull ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long startPeriod = query.getStartTs();
            long endPeriod = Math.max(query.getStartTs() + 1, query.getEndTs());
            long step = Math.max(query.getInterval(), MIN_AGGREGATION_STEP_MS);
            @NotNull List<ListenableFuture<Optional<TsKvEntryAggWrapper>>> futures = new ArrayList<>();
            while (startPeriod < endPeriod) {
                long startTs = startPeriod;
                long endTs = Math.min(startPeriod + step, endPeriod);
                long ts = endTs - startTs;
                @NotNull ReadTsKvQuery subQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, ts, 1, query.getAggregation(), query.getOrder());
                futures.add(findAndAggregateAsync(tenantId, entityId, subQuery, toPartitionTs(startTs), toPartitionTs(endTs)));
                startPeriod = endTs;
            }
            @NotNull ListenableFuture<List<Optional<TsKvEntryAggWrapper>>> future = Futures.allAsList(futures);
            return Futures.transform(future, new Function<>() {
                @Nullable
                @Override
                public ReadTsKvQueryResult apply(@Nullable List<Optional<TsKvEntryAggWrapper>> input) {
                    if (input == null) {
                        return new ReadTsKvQueryResult(query.getId(), Collections.emptyList(), query.getStartTs());
                    } else {
                        long maxTs = query.getStartTs();
                        @NotNull List<TsKvEntry> data = new ArrayList<>();
                        for (@NotNull var opt : input) {
                            if (opt.isPresent()) {
                                @NotNull TsKvEntryAggWrapper tsKvEntryAggWrapper = opt.get();
                                maxTs = Math.max(maxTs, tsKvEntryAggWrapper.getLastEntryTs());
                                data.add(tsKvEntryAggWrapper.getEntry());
                            }
                        }
                        return new ReadTsKvQueryResult(query.getId(), data, maxTs);
                    }

                }
            }, readResultsProcessingExecutor);
        }
    }

    @Override
    public void cleanup(long systemTtl) {
        //Cleanup by TTL is native for Cassandra
    }

    @NotNull
    private ListenableFuture<ReadTsKvQueryResult> findAllAsyncWithLimit(TenantId tenantId, @NotNull EntityId entityId, @NotNull ReadTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());
        @NotNull final ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        @NotNull final SimpleListenableFuture<List<TsKvEntry>> resultFuture = new SimpleListenableFuture<>();

        Futures.addCallback(partitionsListFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Long> partitions) {
                @NotNull TsKvQueryCursor cursor = new TsKvQueryCursor(entityId.getEntityType().name(), entityId.getId(), query, partitions);
                findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to fetch partitions for interval {}-{}", entityId.getEntityType().name(), entityId.getId(), toPartitionTs(query.getStartTs()), toPartitionTs(query.getEndTs()), t);
            }
        }, readResultsProcessingExecutor);

        return Futures.transform(resultFuture, tsKvEntries -> {
            long lastTs = query.getStartTs();
            if (tsKvEntries != null) {
                lastTs = tsKvEntries.stream().map(TsKvEntry::getTs).max(Long::compare).orElse(query.getStartTs());
            }
            return new ReadTsKvQueryResult(query.getId(), tsKvEntries, lastTs);
        }, MoreExecutors.directExecutor());
    }

    long toPartitionTs(long ts) {
        @NotNull LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void findAllAsyncSequentiallyWithLimit(TenantId tenantId, @NotNull final TsKvQueryCursor cursor, @NotNull final SimpleListenableFuture<List<TsKvEntry>> resultFuture) {
        if (cursor.isFull() || !cursor.hasNextPartition()) {
            resultFuture.set(cursor.getData());
        } else {
            PreparedStatement proto = getFetchStmt(Aggregation.NONE, cursor.getOrderBy());
            @NotNull BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());

            stmtBuilder.setString(0, cursor.getEntityType());
            stmtBuilder.setUuid(1, cursor.getEntityId());
            stmtBuilder.setString(2, cursor.getKey());
            stmtBuilder.setLong(3, cursor.getNextPartition());
            stmtBuilder.setLong(4, cursor.getStartTs());
            stmtBuilder.setLong(5, cursor.getEndTs());
            stmtBuilder.setInt(6, cursor.getCurrentLimit());

            @NotNull BoundStatement stmt = stmtBuilder.build();

            Futures.addCallback(executeAsyncRead(tenantId, stmt), new FutureCallback<TbResultSet>() {
                @Override
                public void onSuccess(@Nullable TbResultSet result) {
                    if (result == null) {
                        cursor.addData(convertResultToTsKvEntryList(Collections.emptyList()));
                        findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
                    } else {
                        Futures.addCallback(result.allRows(readResultsProcessingExecutor), new FutureCallback<List<Row>>() {

                            @Override
                            public void onSuccess(@Nullable List<Row> result) {
                                cursor.addData(convertResultToTsKvEntryList(result == null ? Collections.emptyList() : result));
                                findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                            }
                        }, readResultsProcessingExecutor);


                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    @NotNull
    private ListenableFuture<Optional<TsKvEntryAggWrapper>> findAndAggregateAsync(TenantId tenantId, @NotNull EntityId entityId, @NotNull ReadTsKvQuery query, long minPartition, long maxPartition) {
        final Aggregation aggregation = query.getAggregation();
        final String key = query.getKey();
        final long startTs = query.getStartTs();
        final long endTs = query.getEndTs();
        final long ts = startTs + (endTs - startTs) / 2;
        @NotNull ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        @NotNull ListenableFuture<List<TbResultSet>> aggregationChunks = Futures.transformAsync(partitionsListFuture,
                                                                                                getFetchChunksAsyncFunction(tenantId, entityId, key, aggregation, startTs, endTs), readResultsProcessingExecutor);

        return Futures.transformAsync(aggregationChunks, new AggregatePartitionsFunction(aggregation, key, ts, readResultsProcessingExecutor), readResultsProcessingExecutor);
    }

    @NotNull
    private AsyncFunction<TbResultSet, List<Long>> getPartitionsArrayFunction() {
        return rs ->
                Futures.transform(rs.allRows(readResultsProcessingExecutor), rows ->
                                rows.stream()
                                        .map(row -> row.getLong(ModelConstants.PARTITION_COLUMN)).collect(Collectors.toList()),
                        readResultsProcessingExecutor);
    }

    @NotNull
    private ListenableFuture<List<Long>> getPartitionsFuture(TenantId tenantId, @NotNull ReadTsKvQuery query, @NotNull EntityId entityId, long minPartition, long maxPartition) {
        if (isFixedPartitioning()) { //no need to fetch partitions from DB
            return Futures.immediateFuture(FIXED_PARTITION);
        }
        if (!isUseTsKeyValuePartitioningOnRead()) {
            return Futures.immediateFuture(calculatePartitions(minPartition, maxPartition));
        }
        TbResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);
        return Futures.transformAsync(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);
    }

    @NotNull
    List<Long> calculatePartitions(long minPartition, long maxPartition) {
        if (minPartition == maxPartition) {
            return Collections.singletonList(minPartition);
        }
        @NotNull List<Long> partitions = new ArrayList<>();

        long currentPartition = minPartition;
        LocalDateTime currentPartitionTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentPartition), ZoneOffset.UTC);

        while (maxPartition > currentPartition) {
            partitions.add(currentPartition);
            currentPartitionTime = calculateNextPartition(currentPartitionTime);
            currentPartition = currentPartitionTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        partitions.add(maxPartition);

        return partitions;
    }

    private LocalDateTime calculateNextPartition(@NotNull LocalDateTime time) {
        return time.plus(1, tsFormat.getTruncateUnit());
    }

    @NotNull
    private AsyncFunction<List<Long>, List<TbResultSet>> getFetchChunksAsyncFunction(TenantId tenantId, @NotNull EntityId entityId, String key, @NotNull Aggregation aggregation, long startTs, long endTs) {
        return partitions -> {
            try {
                PreparedStatement proto = getFetchStmt(aggregation, DESC_ORDER);
                @NotNull List<TbResultSetFuture> futures = new ArrayList<>(partitions.size());
                for (Long partition : partitions) {
                    log.trace("Fetching data for partition [{}] for entityType {} and entityId {}", partition, entityId.getEntityType(), entityId.getId());
                    @NotNull BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());
                    stmtBuilder.setString(0, entityId.getEntityType().name());
                    stmtBuilder.setUuid(1, entityId.getId());
                    stmtBuilder.setString(2, key);
                    stmtBuilder.setLong(3, partition);
                    stmtBuilder.setLong(4, startTs);
                    stmtBuilder.setLong(5, endTs);
                    @NotNull BoundStatement stmt = stmtBuilder.build();
                    log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
                    futures.add(executeAsyncRead(tenantId, stmt));
                }
                return Futures.allAsList(futures);
            } catch (Throwable e) {
                log.error("Failed to fetch data", e);
                throw e;
            }
        };
    }

    private boolean isFixedPartitioning() {
        return tsFormat.getTruncateUnit().equals(ChronoUnit.FOREVER);
    }

    private void processSetNullValues(TenantId tenantId, @NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry, long ttl, @NotNull List<ListenableFuture<Void>> futures, long partition, @NotNull DataType type) {
        switch (type) {
            case LONG:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case BOOLEAN:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case DOUBLE:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case STRING:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case JSON:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                break;
        }
    }

    private ListenableFuture<Void> saveNull(TenantId tenantId, @NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry, long ttl, long partition, @NotNull DataType type) {
        @NotNull BoundStatementBuilder stmtBuilder = new BoundStatementBuilder((ttl == 0 ? getSaveStmt(type) : getSaveTtlStmt(type)).bind());
        stmtBuilder.setString(0, entityId.getEntityType().name())
                .setUuid(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, partition)
                .setLong(4, tsKvEntry.getTs());
        stmtBuilder.setToNull(getColumnName(type));
        if (ttl > 0) {
            stmtBuilder.setInt(6, (int) ttl);
        }
        @NotNull BoundStatement stmt = stmtBuilder.build();
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
    }

    private ListenableFuture<Integer> doSavePartition(TenantId tenantId, @NotNull EntityId entityId, String key, long ttl, long partition) {
        log.debug("Saving partition {} for the entity [{}-{}] and key {}", partition, entityId.getEntityType(), entityId.getId(), key);
        PreparedStatement preparedStatement = ttl == 0 ? getPartitionInsertStmt() : getPartitionInsertTtlStmt();
        @NotNull BoundStatement stmt = preparedStatement.bind();
        stmt = stmt.setString(0, entityId.getEntityType().name())
                .setUuid(1, entityId.getId())
                .setLong(2, partition)
                .setString(3, key);
        if (ttl > 0) {
            stmt = stmt.setInt(4, (int) ttl);
        }
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> 0);
    }

    private class CacheCallback<Void> implements FutureCallback<Void> {
        private final CassandraPartitionCacheKey key;

        private CacheCallback(CassandraPartitionCacheKey key) {
            this.key = key;
        }

        @Override
        public void onSuccess(Void result) {
            cassandraTsPartitionsCache.put(key);
        }

        @Override
        public void onFailure(Throwable t) {

        }
    }

    private long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    private void deleteAsync(TenantId tenantId, @NotNull final QueryCursor cursor, @NotNull final SimpleListenableFuture<Void> resultFuture) {
        if (!cursor.hasNextPartition()) {
            resultFuture.set(null);
        } else {
            PreparedStatement proto = getDeleteStmt();
            @NotNull BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());
            stmtBuilder.setString(0, cursor.getEntityType());
            stmtBuilder.setUuid(1, cursor.getEntityId());
            stmtBuilder.setString(2, cursor.getKey());
            stmtBuilder.setLong(3, cursor.getNextPartition());
            stmtBuilder.setLong(4, cursor.getStartTs());
            stmtBuilder.setLong(5, cursor.getEndTs());

            @NotNull BoundStatement stmt = stmtBuilder.build();

            Futures.addCallback(executeAsyncWrite(tenantId, stmt), new FutureCallback<AsyncResultSet>() {
                @Override
                public void onSuccess(@Nullable AsyncResultSet result) {
                    deleteAsync(tenantId, cursor, resultFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to delete data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    private PreparedStatement getDeleteStmt() {
        if (deleteStmt == null) {
            stmtCreationLock.lock();
            try {
                if (deleteStmt == null) {
                    deleteStmt = prepare("DELETE FROM " + ModelConstants.TS_KV_CF +
                            " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.TS_COLUMN + " >= ? "
                            + "AND " + ModelConstants.TS_COLUMN + " < ?");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return deleteStmt;
    }

    private PreparedStatement getSaveStmt(@NotNull DataType dataType) {
        if (saveStmts == null) {
            stmtCreationLock.lock();
            try {
                if (saveStmts == null) {
                    @NotNull var stmts = new PreparedStatement[DataType.values().length];
                    for (@NotNull DataType type : DataType.values()) {
                        stmts[type.ordinal()] = prepare(getPreparedStatementQuery(type));
                    }
                    saveStmts = stmts;
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveStmts[dataType.ordinal()];
    }

    private PreparedStatement getSaveTtlStmt(@NotNull DataType dataType) {
        if (saveTtlStmts == null) {
            stmtCreationLock.lock();
            try {
                if (saveTtlStmts == null) {
                    @NotNull var stmts = new PreparedStatement[DataType.values().length];
                    for (@NotNull DataType type : DataType.values()) {
                        stmts[type.ordinal()] = prepare(getPreparedStatementQueryWithTtl(type));
                    }
                    saveTtlStmts = stmts;
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveTtlStmts[dataType.ordinal()];
    }

    @NotNull
    private String getPreparedStatementQuery(@NotNull DataType type) {
        return INSERT_INTO + ModelConstants.TS_KV_CF +
                "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                "," + ModelConstants.ENTITY_ID_COLUMN +
                "," + ModelConstants.KEY_COLUMN +
                "," + ModelConstants.PARTITION_COLUMN +
                "," + ModelConstants.TS_COLUMN +
                "," + getColumnName(type) + ")" +
                " VALUES(?, ?, ?, ?, ?, ?)";
    }

    @NotNull
    private String getPreparedStatementQueryWithTtl(@NotNull DataType type) {
        return getPreparedStatementQuery(type) + " USING TTL ?";
    }

    private PreparedStatement getPartitionInsertStmt() {
        if (partitionInsertStmt == null) {
            stmtCreationLock.lock();
            try {
                if (partitionInsertStmt == null) {
                    partitionInsertStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                            "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                            "," + ModelConstants.ENTITY_ID_COLUMN +
                            "," + ModelConstants.PARTITION_COLUMN +
                            "," + ModelConstants.KEY_COLUMN + ")" +
                            " VALUES(?, ?, ?, ?)");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return partitionInsertStmt;
    }

    private PreparedStatement getPartitionInsertTtlStmt() {
        if (partitionInsertTtlStmt == null) {
            stmtCreationLock.lock();
            try {
                if (partitionInsertTtlStmt == null) {
                    partitionInsertTtlStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                            "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                            "," + ModelConstants.ENTITY_ID_COLUMN +
                            "," + ModelConstants.PARTITION_COLUMN +
                            "," + ModelConstants.KEY_COLUMN + ")" +
                            " VALUES(?, ?, ?, ?) USING TTL ?");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return partitionInsertTtlStmt;
    }

    @NotNull
    private static String getColumnName(@NotNull DataType type) {
        switch (type) {
            case BOOLEAN:
                return ModelConstants.BOOLEAN_VALUE_COLUMN;
            case STRING:
                return ModelConstants.STRING_VALUE_COLUMN;
            case LONG:
                return ModelConstants.LONG_VALUE_COLUMN;
            case DOUBLE:
                return ModelConstants.DOUBLE_VALUE_COLUMN;
            case JSON:
                return ModelConstants.JSON_VALUE_COLUMN;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private static void addValue(@NotNull KvEntry kvEntry, @NotNull BoundStatementBuilder stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = kvEntry.getBooleanValue();
                booleanValue.ifPresent(b -> stmt.setBoolean(column, b));
                break;
            case STRING:
                Optional<String> stringValue = kvEntry.getStrValue();
                stringValue.ifPresent(s -> stmt.setString(column, s));
                break;
            case LONG:
                Optional<Long> longValue = kvEntry.getLongValue();
                longValue.ifPresent(l -> stmt.setLong(column, l));
                break;
            case DOUBLE:
                Optional<Double> doubleValue = kvEntry.getDoubleValue();
                doubleValue.ifPresent(d -> stmt.setDouble(column, d));
                break;
            case JSON:
                Optional<String> jsonValue = kvEntry.getJsonValue();
                jsonValue.ifPresent(jsonObject -> stmt.setString(column, jsonObject));
                break;
        }
    }

    /**
     * //     * Select existing partitions from the table
     * //     * <code>{@link ModelConstants#TS_KV_PARTITIONS_CF}</code> for the given entity
     * //
     */
    private TbResultSetFuture fetchPartitions(TenantId tenantId, @NotNull EntityId entityId, String key, long minPartition, long maxPartition) {
        @NotNull Select select = QueryBuilder.selectFrom(ModelConstants.TS_KV_PARTITIONS_CF).column(ModelConstants.PARTITION_COLUMN)
                                             .whereColumn(ModelConstants.ENTITY_TYPE_COLUMN).isEqualTo(literal(entityId.getEntityType().name()))
                                             .whereColumn(ModelConstants.ENTITY_ID_COLUMN).isEqualTo(literal(entityId.getId()))
                                             .whereColumn(ModelConstants.KEY_COLUMN).isEqualTo(literal(key))
                                             .whereColumn(ModelConstants.PARTITION_COLUMN).isGreaterThanOrEqualTo(literal(minPartition))
                                             .whereColumn(ModelConstants.PARTITION_COLUMN).isLessThanOrEqualTo(literal(maxPartition));
        return executeAsyncRead(tenantId, select.build());
    }

    private PreparedStatement getFetchStmt(@NotNull Aggregation aggType, @NotNull String orderBy) {
        switch (orderBy) {
            case ASC_ORDER:
                if (fetchStmtsAsc == null) {
                    stmtCreationLock.lock();
                    try {
                        if (fetchStmtsAsc == null) {
                            fetchStmtsAsc = initFetchStmt(orderBy);
                        }
                    } finally {
                        stmtCreationLock.unlock();
                    }
                }
                return fetchStmtsAsc[aggType.ordinal()];
            case DESC_ORDER:
                if (fetchStmtsDesc == null) {
                    stmtCreationLock.lock();
                    try {
                        if (fetchStmtsDesc == null) {
                            fetchStmtsDesc = initFetchStmt(orderBy);
                        }
                    } finally {
                        stmtCreationLock.unlock();
                    }
                }
                return fetchStmtsDesc[aggType.ordinal()];
            default:
                throw new RuntimeException("Not supported" + orderBy + "order!");
        }
    }

    @NotNull
    private PreparedStatement[] initFetchStmt(String orderBy) {
        @NotNull PreparedStatement[] fetchStmts = new PreparedStatement[Aggregation.values().length];
        for (@NotNull Aggregation type : Aggregation.values()) {
            if (type == Aggregation.SUM && fetchStmts[Aggregation.AVG.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.AVG.ordinal()];
            } else if (type == Aggregation.AVG && fetchStmts[Aggregation.SUM.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.SUM.ordinal()];
            } else {
                fetchStmts[type.ordinal()] = prepare(SELECT_PREFIX +
                        String.join(", ", ModelConstants.getFetchColumnNames(type)) + " FROM " + ModelConstants.TS_KV_CF
                        + " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.TS_COLUMN + " >= ? "
                        + "AND " + ModelConstants.TS_COLUMN + " < ?"
                        + (type == Aggregation.NONE ? " ORDER BY " + ModelConstants.TS_COLUMN + " " + orderBy + " LIMIT ?" : ""));
            }
        }
        return fetchStmts;
    }
}
