package org.echoiot.server.dao.sqlts.timescale;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.AbstractTsKvEntity;
import org.echoiot.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueParams;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.echoiot.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.echoiot.server.dao.sqlts.insert.InsertTsRepository;
import org.echoiot.server.dao.timeseries.TimeseriesDao;
import org.echoiot.server.dao.util.TimescaleDBTsDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    @Resource
    private TsKvTimescaleRepository tsKvRepository;

    @Resource
    private AggregationRepository aggregationRepository;

    @Resource
    private StatsFactory statsFactory;

    @Resource
    protected InsertTsRepository<TimescaleTsKvEntity> insertRepository;

    protected TbSqlBlockingQueueWrapper<TimescaleTsKvEntity> tsQueue;

    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                                                                    .logName("TS Timescale")
                                                                    .batchSize(tsBatchSize)
                                                                    .maxDelay(tsMaxDelay)
                                                                    .statsPrintIntervalMs(tsStatsPrintIntervalMs)
                                                                    .statsNamePrefix("ts.timescale")
                                                                    .batchSortEnabled(batchSortEnabled)
                                                                    .build();

        @NotNull Function<TimescaleTsKvEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsQueue = new TbSqlBlockingQueueWrapper<>(tsParams, hashcodeFunction, timescaleBatchThreads, statsFactory);

        tsQueue.init(logExecutor, v -> insertRepository.saveOrUpdate(v),
                Comparator.comparing((Function<TimescaleTsKvEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparing(AbstractTsKvEntity::getKey)
                        .thenComparing(AbstractTsKvEntity::getTs)
        );
    }

    @PreDestroy
    protected void destroy() {
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, @NotNull List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    @NotNull
    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, @NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry, computeTtl(ttl));
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        @NotNull TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        entity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));
        log.trace("Saving entity to timescale db: {}", entity);
        return Futures.transform(tsQueue.add(entity), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key) {
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, @NotNull EntityId entityId, @NotNull DeleteTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        return service.submit(() -> {
            tsKvRepository.delete(
                    entityId.getId(),
                    keyId,
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, @NotNull EntityId entityId, @NotNull ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return Futures.immediateFuture(findAllAsyncWithLimit(entityId, query));
        } else {
            long startTs = query.getStartTs();
            long endTs = Math.max(query.getStartTs() + 1, query.getEndTs());
            long timeBucket = query.getInterval();
            @NotNull List<Optional<? extends AbstractTsKvEntity>> data = findAllAndAggregateAsync(entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getReadTsKvQueryResultFuture(query, Futures.immediateFuture(data));
        }
    }

    @Override
    public void cleanup(long systemTtl) {
        super.cleanup(systemTtl);
    }

    @NotNull
    private ReadTsKvQueryResult findAllAsyncWithLimit(@NotNull EntityId entityId, @NotNull ReadTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        List<TimescaleTsKvEntity> timescaleTsKvEntities = tsKvRepository.findAllWithLimit(
                entityId.getId(),
                keyId,
                query.getStartTs(),
                query.getEndTs(),
                PageRequest.ofSize(query.getLimit()).withSort(Sort.Direction.fromString(query.getOrder()), "ts"));
        timescaleTsKvEntities.forEach(tsKvEntity -> tsKvEntity.setStrKey(strKey));
        var tsKvEntries = DaoUtil.convertDataList(timescaleTsKvEntities);
        long lastTs = tsKvEntries.stream().map(TsKvEntry::getTs).max(Long::compare).orElse(query.getStartTs());
        return new ReadTsKvQueryResult(query.getId(), tsKvEntries, lastTs);
    }

    @NotNull
    private List<Optional<? extends AbstractTsKvEntity>> findAllAndAggregateAsync(@NotNull EntityId entityId, String key, long startTs, long endTs, long timeBucket, @NotNull Aggregation aggregation) {
        long interval = endTs - startTs;
        long remainingPart = interval % timeBucket;
        List<TimescaleTsKvEntity> timescaleTsKvEntities;
        if (remainingPart == 0) {
            timescaleTsKvEntities = switchAggregation(key, startTs, endTs, timeBucket, aggregation, entityId.getId());
        } else {
            interval = interval - remainingPart;
            timescaleTsKvEntities = new ArrayList<>();
            timescaleTsKvEntities.addAll(switchAggregation(key, startTs, startTs + interval, timeBucket, aggregation, entityId.getId()));
            timescaleTsKvEntities.addAll(switchAggregation(key, startTs + interval, endTs, remainingPart, aggregation, entityId.getId()));
        }

        if (!CollectionUtils.isEmpty(timescaleTsKvEntities)) {
            @NotNull List<Optional<? extends AbstractTsKvEntity>> result = new ArrayList<>();
            timescaleTsKvEntities.forEach(entity -> {
                if (entity != null && entity.isNotEmpty()) {
                    entity.setEntityId(entityId.getId());
                    entity.setStrKey(key);
                    result.add(Optional.of(entity));
                } else {
                    result.add(Optional.empty());
                }
            });
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private List<TimescaleTsKvEntity> switchAggregation(String key, long startTs, long endTs, long timeBucket, @NotNull Aggregation aggregation, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        switch (aggregation) {
            case AVG:
                return aggregationRepository.findAvg(entityId, keyId, timeBucket, startTs, endTs);
            case MAX:
                return aggregationRepository.findMax(entityId, keyId, timeBucket, startTs, endTs);
            case MIN:
                return aggregationRepository.findMin(entityId, keyId, timeBucket, startTs, endTs);
            case SUM:
                return aggregationRepository.findSum(entityId, keyId, timeBucket, startTs, endTs);
            case COUNT:
                return aggregationRepository.findCount(entityId, keyId, timeBucket, startTs, endTs);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

}
