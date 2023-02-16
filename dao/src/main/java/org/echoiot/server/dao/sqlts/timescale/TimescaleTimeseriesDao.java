package org.echoiot.server.dao.sqlts.timescale;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.echoiot.server.common.data.kv.Aggregation;
import org.echoiot.server.common.data.kv.DeleteTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQueryResult;
import org.echoiot.server.common.data.kv.TsKvEntry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

    @Autowired
    private AggregationRepository aggregationRepository;

    @Autowired
    private StatsFactory statsFactory;

    @Autowired
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

        Function<TimescaleTsKvEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
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
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry, computeTtl(ttl));
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
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

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key) {
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
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
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return Futures.immediateFuture(findAllAsyncWithLimit(entityId, query));
        } else {
            long startTs = query.getStartTs();
            long endTs = Math.max(query.getStartTs() + 1, query.getEndTs());
            long timeBucket = query.getInterval();
            List<Optional<? extends AbstractTsKvEntity>> data = findAllAndAggregateAsync(entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getReadTsKvQueryResultFuture(query, Futures.immediateFuture(data));
        }
    }

    @Override
    public void cleanup(long systemTtl) {
        super.cleanup(systemTtl);
    }

    private ReadTsKvQueryResult findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
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

    private List<Optional<? extends AbstractTsKvEntity>> findAllAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long timeBucket, Aggregation aggregation) {
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
            List<Optional<? extends AbstractTsKvEntity>> result = new ArrayList<>();
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

    private List<TimescaleTsKvEntity> switchAggregation(String key, long startTs, long endTs, long timeBucket, Aggregation aggregation, UUID entityId) {
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
