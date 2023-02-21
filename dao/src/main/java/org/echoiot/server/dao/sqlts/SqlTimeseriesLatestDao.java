package org.echoiot.server.dao.sqlts;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.AbstractTsKvEntity;
import org.echoiot.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.echoiot.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.echoiot.server.dao.sql.ScheduledLogExecutorComponent;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueParams;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.echoiot.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.echoiot.server.dao.sqlts.latest.SearchTsKvLatestRepository;
import org.echoiot.server.dao.sqlts.latest.TsKvLatestRepository;
import org.echoiot.server.dao.timeseries.TimeseriesLatestDao;
import org.echoiot.server.dao.util.SqlTsLatestAnyDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlTsLatestAnyDao
public class SqlTimeseriesLatestDao extends BaseAbstractSqlTimeseriesDao implements TimeseriesLatestDao {

    private static final String DESC_ORDER = "DESC";

    @Resource
    private TsKvLatestRepository tsKvLatestRepository;

    @Resource
    protected AggregationTimeseriesDao aggregationTimeseriesDao;

    @Resource
    private SearchTsKvLatestRepository searchTsKvLatestRepository;

    @Resource
    private InsertLatestTsRepository insertLatestTsRepository;

    private TbSqlBlockingQueueWrapper<TsKvLatestEntity> tsLatestQueue;

    @Value("${sql.ts_latest.batch_size:1000}")
    private int tsLatestBatchSize;

    @Value("${sql.ts_latest.batch_max_delay:100}")
    private long tsLatestMaxDelay;

    @Value("${sql.ts_latest.stats_print_interval_ms:1000}")
    private long tsLatestStatsPrintIntervalMs;

    @Value("${sql.ts_latest.batch_threads:4}")
    private int tsLatestBatchThreads;

    @Value("${sql.batch_sort:true}")
    protected boolean batchSortEnabled;

    @Resource
    protected ScheduledLogExecutorComponent logExecutor;

    @Resource
    private StatsFactory statsFactory;

    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsLatestParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Latest")
                .batchSize(tsLatestBatchSize)
                .maxDelay(tsLatestMaxDelay)
                .statsPrintIntervalMs(tsLatestStatsPrintIntervalMs)
                .statsNamePrefix("ts.latest")
                .batchSortEnabled(false)
                .build();

        @NotNull java.util.function.Function<TsKvLatestEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsLatestQueue = new TbSqlBlockingQueueWrapper<>(tsLatestParams, hashcodeFunction, tsLatestBatchThreads, statsFactory);

        tsLatestQueue.init(logExecutor, v -> {
            @NotNull Map<TsKey, TsKvLatestEntity> trueLatest = new HashMap<>();
            v.forEach(ts -> {
                @NotNull TsKey key = new TsKey(ts.getEntityId(), ts.getKey());
                trueLatest.merge(key, ts, (oldTs, newTs) -> oldTs.getTs() <= newTs.getTs() ? newTs : oldTs);
            });
            @NotNull List<TsKvLatestEntity> latestEntities = new ArrayList<>(trueLatest.values());
            if (batchSortEnabled) {
                latestEntities.sort(Comparator.comparing((Function<TsKvLatestEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparingInt(AbstractTsKvEntity::getKey));
            }
            insertLatestTsRepository.saveOrUpdate(latestEntities);
        }, (l, r) -> 0);
    }

    @PreDestroy
    protected void destroy() {
        if (tsLatestQueue != null) {
            tsLatestQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, @NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, @NotNull EntityId entityId, @NotNull DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @NotNull
    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, @NotNull EntityId entityId, String key) {
        return Futures.immediateFuture(Optional.ofNullable(doFindLatest(entityId, key)));
    }

    @NotNull
    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, @NotNull EntityId entityId, String key) {
        @Nullable TsKvEntry latest = doFindLatest(entityId, key);
        if (latest == null) {
            latest = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return Futures.immediateFuture(latest);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, @NotNull EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(@NotNull TenantId tenantId, @Nullable DeviceProfileId deviceProfileId) {
        if (deviceProfileId != null) {
            return tsKvLatestRepository.getKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId());
        } else {
            return tsKvLatestRepository.getKeysByTenantId(tenantId.getId());
        }
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, @NotNull List<EntityId> entityIds) {
        return tsKvLatestRepository.findAllKeysByEntityIds(entityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
    }

    @NotNull
    private ListenableFuture<TsKvLatestRemovingResult> getNewLatestEntryFuture(TenantId tenantId, @NotNull EntityId entityId, @NotNull DeleteTsKvQuery query) {
        @NotNull ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                TsKvEntry entry = entryList.get(0);
                return Futures.transform(getSaveLatestFuture(entityId, entry), v -> new TsKvLatestRemovingResult(entry), MoreExecutors.directExecutor());
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), true));
        }, service);
    }

    @NotNull
    private ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, @NotNull DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        @NotNull ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                                                                          Aggregation.NONE, DESC_ORDER);
        return Futures.transform(aggregationTimeseriesDao.findAllAsync(tenantId, entityId, findNewLatestQuery),
                ReadTsKvQueryResult::getData, MoreExecutors.directExecutor());
    }

   @Nullable
   protected TsKvEntry doFindLatest(@NotNull EntityId entityId, String key) {
        @NotNull TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getId(),
                        getOrSaveKeyId(key));
        @NotNull Optional<TsKvLatestEntity> entry = tsKvLatestRepository.findById(compositeKey);
        if (entry.isPresent()) {
            @NotNull TsKvLatestEntity tsKvLatestEntity = entry.get();
            tsKvLatestEntity.setStrKey(key);
            return DaoUtil.getData(tsKvLatestEntity);
        } else {
            return null;
        }
    }

    @NotNull
    protected ListenableFuture<TsKvLatestRemovingResult> getRemoveLatestFuture(TenantId tenantId, @NotNull EntityId entityId, @NotNull DeleteTsKvQuery query) {
        @Nullable TsKvEntry latest = doFindLatest(entityId, query.getKey());

        if (latest == null) {
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), false));
        }

        long ts = latest.getTs();
        ListenableFuture<Boolean> removedLatestFuture;
        if (ts > query.getStartTs() && ts <= query.getEndTs()) {
            @NotNull TsKvLatestEntity latestEntity = new TsKvLatestEntity();
            latestEntity.setEntityId(entityId.getId());
            latestEntity.setKey(getOrSaveKeyId(query.getKey()));
            removedLatestFuture = service.submit(() -> {
                tsKvLatestRepository.delete(latestEntity);
                return true;
            });
        } else {
            removedLatestFuture = Futures.immediateFuture(false);
        }

        return Futures.transformAsync(removedLatestFuture, isRemoved -> {
            if (isRemoved && query.getRewriteLatestIfDeleted()) {
                return getNewLatestEntryFuture(tenantId, entityId, query);
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), isRemoved));
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    protected ListenableFuture<List<TsKvEntry>> getFindAllLatestFuture(@NotNull EntityId entityId) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        searchTsKvLatestRepository.findAllByEntityId(entityId.getId()))));
    }

    protected ListenableFuture<Void> getSaveLatestFuture(@NotNull EntityId entityId, @NotNull TsKvEntry tsKvEntry) {
        @NotNull TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(entityId.getId());
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(getOrSaveKeyId(tsKvEntry.getKey()));
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        latestEntity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));

        return tsLatestQueue.add(latestEntity);
    }

}
