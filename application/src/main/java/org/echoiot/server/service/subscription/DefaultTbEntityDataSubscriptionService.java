package org.echoiot.server.service.subscription;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.kv.BaseReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQueryResult;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.echoiot.server.service.telemetry.cmd.v2.*;
import org.echoiot.server.service.telemetry.sub.SubscriptionErrorCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
@TbCoreComponent
@Service
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    private static final int DEFAULT_LIMIT = 100;
    private final Map<String, Map<Integer, TbAbstractSubCtx>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Resource
    private TelemetryWebSocketService wsService;

    @Resource
    private EntityService entityService;

    @Resource
    private AlarmService alarmService;

    @Resource
    private AttributesService attributesService;

    @Resource
    @Lazy
    private TbLocalSubscriptionService localSubscriptionService;

    @Resource
    private TimeseriesService tsService;

    @Resource
    private TbServiceInfoProvider serviceInfoProvider;

    @Resource
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    private ScheduledExecutorService scheduler;

    @Value("${database.ts.type}")
    private String databaseTsType;
    @Value("${server.ws.dynamic_page_link.refresh_interval:6}")
    private long dynamicPageLinkRefreshInterval;
    @Value("${server.ws.dynamic_page_link.refresh_pool_size:1}")
    private int dynamicPageLinkRefreshPoolSize;
    @Value("${server.ws.max_entities_per_data_subscription:1000}")
    private int maxEntitiesPerDataSubscription;
    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;
    @Value("${server.ws.dynamic_page_link.max_alarm_queries_per_refresh_interval:10}")
    private int maxAlarmQueriesPerRefreshInterval;
    @Value("${ui.dashboard.max_datapoints_limit:50000}")
    private int maxDatapointLimit;

    private ExecutorService wsCallBackExecutor;
    private boolean tsInSqlDB;
    private String serviceId;
    private final SubscriptionServiceStatistics stats = new SubscriptionServiceStatistics();

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        wsCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("ws-entity-sub-callback"));
        tsInSqlDB = databaseTsType.equalsIgnoreCase("sql") || databaseTsType.equalsIgnoreCase("timescale");
        @NotNull ThreadFactory tbThreadFactory = EchoiotThreadFactory.forName("ws-entity-sub-scheduler");
        if (dynamicPageLinkRefreshPoolSize == 1) {
            scheduler = Executors.newSingleThreadScheduledExecutor(tbThreadFactory);
        } else {
            scheduler = Executors.newScheduledThreadPool(dynamicPageLinkRefreshPoolSize, tbThreadFactory);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void handleCmd(@NotNull TelemetryWebSocketSessionRef session, @NotNull EntityDataCmd cmd) {
        @org.jetbrains.annotations.Nullable TbEntityDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx != null) {
            log.debug("[{}][{}] Updating existing subscriptions using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            if (cmd.hasAnyCmd()) {
                ctx.clearEntitySubscriptions();
            }
        } else {
            log.debug("[{}][{}] Creating new subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setCurrentCmd(cmd);

        // Fetch entity list using entity data query
        if (cmd.getQuery() != null) {
            if (ctx.getQuery() == null) {
                log.debug("[{}][{}] Initializing data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            } else {
                log.debug("[{}][{}] Updating data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            }
            ctx.setAndResolveQuery(cmd.getQuery());
            EntityDataQuery query = ctx.getQuery();
            //Step 1. Update existing query with the contents of LatestValueCmd
            if (cmd.getLatestCmd() != null) {
                cmd.getLatestCmd().getKeys().forEach(key -> {
                    if (!query.getLatestValues().contains(key)) {
                        query.getLatestValues().add(key);
                    }
                });
            }
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            ctx.cancelTasks();
            if (ctx.getQuery().getPageLink().isDynamic()) {
                //TODO: validate number of dynamic page links against rate limits. Ignore dynamic flag if limit is reached.
                @NotNull TbEntityDataSubCtx finalCtx = ctx;
                @NotNull ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshDynamicQuery(finalCtx),
                        dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }

        try {
            @NotNull List<ListenableFuture<?>> cmdFutures = new ArrayList<>();
            if (cmd.getAggHistoryCmd() != null) {
                cmdFutures.add(handleAggHistoryCmd(ctx, cmd.getAggHistoryCmd()));
            }
            if (cmd.getAggTsCmd() != null) {
                cmdFutures.add(handleAggTsCmd(ctx, cmd.getAggTsCmd()));
            }
            if (cmd.getHistoryCmd() != null) {
                cmdFutures.add(handleHistoryCmd(ctx, cmd.getHistoryCmd()));
            }
            if (cmdFutures.isEmpty()) {
                handleRegularCommands(ctx, cmd);
            } else {
                @NotNull TbEntityDataSubCtx finalCtx = ctx;
                Futures.addCallback(Futures.allAsList(cmdFutures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Object> result) {
                        handleRegularCommands(finalCtx, cmd);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}][{}] Failed to process command", finalCtx.getSessionId(), finalCtx.getCmdId());
                    }
                }, wsCallBackExecutor);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    private void handleRegularCommands(@NotNull TbEntityDataSubCtx ctx, @NotNull EntityDataCmd cmd) {
        try {
            if (cmd.getLatestCmd() != null || cmd.getTsCmd() != null) {
                if (cmd.getLatestCmd() != null) {
                    handleLatestCmd(ctx, cmd.getLatestCmd());
                }
                if (cmd.getTsCmd() != null) {
                    handleTimeSeriesCmd(ctx, cmd.getTsCmd());
                }
            } else {
                checkAndSendInitialData(ctx);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    private void checkAndSendInitialData(@Nullable TbEntityDataSubCtx theCtx) {
        if (!theCtx.isInitialDataSent()) {
            @NotNull EntityDataUpdate update = new EntityDataUpdate(theCtx.getCmdId(), theCtx.getData(), null, theCtx.getMaxEntitiesPerDataSubscription());
            theCtx.sendWsMsg(update);
            theCtx.setInitialDataSent(true);
        }
    }

    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleAggHistoryCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull AggHistoryCmd cmd) {
        @NotNull ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (@NotNull AggKey key : cmd.getKeys()) {
            if (key.getPreviousValueOnly() == null || !key.getPreviousValueOnly()) {
                @NotNull var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getEndTs(), cmd.getEndTs() - cmd.getStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
            }
            if (key.getPreviousStartTs() != null && key.getPreviousEndTs() != null && key.getPreviousEndTs() >= key.getPreviousStartTs()) {
                @NotNull var query = new BaseReadTsKvQuery(key.getKey(), key.getPreviousStartTs(), key.getPreviousEndTs(), key.getPreviousEndTs() - key.getPreviousStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, true));
            }
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getEndTs(), false);
    }

    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleAggTsCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull AggTimeSeriesCmd cmd) {
        @NotNull ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (@NotNull AggKey key : cmd.getKeys()) {
            @NotNull var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), cmd.getTimeWindow(), 1, key.getAgg());
            queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), true);
    }

    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleAggCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull List<AggKey> keys, @NotNull ConcurrentMap<Integer, ReadTsKvQueryInfo> queries,
                                                              long startTs, long endTs, boolean subscribe) {
        @NotNull Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        @NotNull List<ReadTsKvQuery> queryList = queries.values().stream().map(ReadTsKvQueryInfo::getQuery).collect(Collectors.toList());
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), queryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            @NotNull Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    @NotNull Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (@NotNull ReadTsKvQueryResult queryResult : queryResults) {
                            ReadTsKvQueryInfo queryInfo = queries.get(queryResult.getQueryId());
                            @NotNull ComparisonTsValue comparisonTsValue = entityData.getAggLatest().computeIfAbsent(queryInfo.getKey().getId(), agg -> new ComparisonTsValue());
                            if (queryInfo.isPrevious()) {
                                comparisonTsValue.setPrevious(queryResult.toTsValue(queryInfo.getQuery()));
                            } else {
                                comparisonTsValue.setCurrent(queryResult.toTsValue(queryInfo.getQuery()));
                                lastTsMap.put(queryInfo.getQuery().getKey(), queryResult.getLastEntryTs());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        entityData.getAggLatest().putIfAbsent(key.getId(), new ComparisonTsValue(TsValue.EMPTY, TsValue.EMPTY));
                    });
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, startTs, endTs, true);
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    private void handleWsCmdRuntimeException(String sessionId, RuntimeException e, EntityDataCmd cmd) {
        log.debug("[{}] Failed to process ws cmd: {}", sessionId, cmd, e);
        wsService.close(sessionId, CloseStatus.SERVICE_RESTARTED);
    }

    @Override
    public void handleCmd(@NotNull TelemetryWebSocketSessionRef session, @NotNull EntityCountCmd cmd) {
        @org.jetbrains.annotations.Nullable TbEntityCountSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            @NotNull TbEntityCountSubCtx finalCtx = ctx;
            @NotNull ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> refreshDynamicQuery(finalCtx),
                    dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
            finalCtx.setRefreshTask(task);
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    @Override
    public void handleCmd(@NotNull TelemetryWebSocketSessionRef session, @NotNull AlarmDataCmd cmd) {
        @org.jetbrains.annotations.Nullable TbAlarmDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            log.debug("[{}][{}] Creating new alarm subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setAndResolveQuery(cmd.getQuery());
        AlarmDataQuery adq = ctx.getQuery();
        long start = System.currentTimeMillis();
        ctx.fetchData();
        long end = System.currentTimeMillis();
        stats.getRegularQueryInvocationCnt().incrementAndGet();
        stats.getRegularQueryTimeSpent().addAndGet(end - start);
        List<EntityData> entities = ctx.getEntitiesData();
        ctx.cancelTasks();
        ctx.clearEntitySubscriptions();
        if (entities.isEmpty()) {
            @NotNull AlarmDataUpdate update = new AlarmDataUpdate(cmd.getCmdId(), new PageData<>(), null, 0, 0);
            ctx.sendWsMsg(update);
        } else {
            ctx.fetchAlarms();
            ctx.createLatestValuesSubscriptions(cmd.getQuery().getLatestValues());
            if (adq.getPageLink().getTimeWindow() > 0) {
                @NotNull TbAlarmDataSubCtx finalCtx = ctx;
                @NotNull ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshAlarmQuery(finalCtx), dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }
    }

    private boolean validate(@NotNull TbAbstractSubCtx<?> finalCtx) {
        if (finalCtx.isStopped()) {
            log.warn("[{}][{}][{}] Received validation task for already stopped context.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        var cmdMap = subscriptionsBySessionId.get(finalCtx.getSessionId());
        if (cmdMap == null) {
            log.warn("[{}][{}][{}] Received validation task for already removed session.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        } else if (!cmdMap.containsKey(finalCtx.getCmdId())) {
            log.warn("[{}][{}][{}] Received validation task for unregistered cmdId.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        return true;
    }

    private void refreshDynamicQuery(@NotNull TbAbstractSubCtx<?> finalCtx) {
        try {
            if (validate(finalCtx)) {
                long start = System.currentTimeMillis();
                finalCtx.update();
                long end = System.currentTimeMillis();
                log.trace("[{}][{}] Executing query: {}", finalCtx.getSessionId(), finalCtx.getCmdId(), finalCtx.getQuery());
                stats.getDynamicQueryInvocationCnt().incrementAndGet();
                stats.getDynamicQueryTimeSpent().addAndGet(end - start);
            } else {
                finalCtx.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to refresh query", finalCtx.getSessionId(), finalCtx.getCmdId(), e);
        }
    }

    private void refreshAlarmQuery(@NotNull TbAlarmDataSubCtx finalCtx) {
        if (validate(finalCtx)) {
            finalCtx.checkAndResetInvocationCounter();
        } else {
            finalCtx.stop();
        }
    }

    @Scheduled(fixedDelayString = "${server.ws.dynamic_page_link.stats:10000}")
    public void printStats() {
        int alarmQueryInvocationCntValue = stats.getAlarmQueryInvocationCnt().getAndSet(0);
        long alarmQueryInvocationTimeValue = stats.getAlarmQueryTimeSpent().getAndSet(0);
        int regularQueryInvocationCntValue = stats.getRegularQueryInvocationCnt().getAndSet(0);
        long regularQueryInvocationTimeValue = stats.getRegularQueryTimeSpent().getAndSet(0);
        int dynamicQueryInvocationCntValue = stats.getDynamicQueryInvocationCnt().getAndSet(0);
        long dynamicQueryInvocationTimeValue = stats.getDynamicQueryTimeSpent().getAndSet(0);
        long dynamicQueryCnt = subscriptionsBySessionId.values().stream().mapToLong(m -> m.values().stream().filter(TbAbstractSubCtx::isDynamic).count()).sum();
        if (regularQueryInvocationCntValue > 0 || dynamicQueryInvocationCntValue > 0 || dynamicQueryCnt > 0 || alarmQueryInvocationCntValue > 0) {
            log.info("Stats: regularQueryInvocationCnt = [{}], regularQueryInvocationTime = [{}], " +
                            "dynamicQueryCnt = [{}] dynamicQueryInvocationCnt = [{}], dynamicQueryInvocationTime = [{}], " +
                            "alarmQueryInvocationCnt = [{}], alarmQueryInvocationTime = [{}]",
                    regularQueryInvocationCntValue, regularQueryInvocationTimeValue,
                    dynamicQueryCnt, dynamicQueryInvocationCntValue, dynamicQueryInvocationTimeValue,
                    alarmQueryInvocationCntValue, alarmQueryInvocationTimeValue);
        }
    }

    @NotNull
    private TbEntityDataSubCtx createSubCtx(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityDataCmd cmd) {
        @NotNull Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        @NotNull TbEntityDataSubCtx ctx = new TbEntityDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                                                                 attributesService, stats, sessionRef, cmd.getCmdId(), maxEntitiesPerDataSubscription);
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    @NotNull
    private TbEntityCountSubCtx createSubCtx(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityCountCmd cmd) {
        @NotNull Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        @NotNull TbEntityCountSubCtx ctx = new TbEntityCountSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                                                                   attributesService, stats, sessionRef, cmd.getCmdId());
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }


    @NotNull
    private TbAlarmDataSubCtx createSubCtx(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull AlarmDataCmd cmd) {
        @NotNull Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        @NotNull TbAlarmDataSubCtx ctx = new TbAlarmDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                                                               attributesService, stats, alarmService, sessionRef, cmd.getCmdId(), maxEntitiesPerAlarmSubscription,
                                                               maxAlarmQueriesPerRefreshInterval);
        ctx.setAndResolveQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    @org.jetbrains.annotations.Nullable
    @SuppressWarnings("unchecked")
    private <T extends TbAbstractSubCtx> T getSubCtx(String sessionId, int cmdId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(sessionId);
        if (sessionSubs != null) {
            return (T) sessionSubs.get(cmdId);
        } else {
            return null;
        }
    }

    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleTimeSeriesCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull TimeSeriesCmd cmd) {
        log.debug("[{}][{}] Fetching time-series data for last {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getTimeWindow(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, true);
    }


    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleHistoryCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull EntityHistoryCmd cmd) {
        log.debug("[{}][{}] Fetching history data for start {} and end {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getStartTs(), cmd.getEndTs(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, false);
    }

    @NotNull
    private ListenableFuture<TbEntityDataSubCtx> handleGetTsCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull GetTsCmd cmd, boolean subscribe) {
        @NotNull Map<Integer, String> queriesKeys = new ConcurrentHashMap<>();

        List<String> keys = cmd.getKeys();
        List<ReadTsKvQuery> finalTsKvQueryList;
        @NotNull List<ReadTsKvQuery> tsKvQueryList = keys.stream().map(key -> {
            @NotNull var query = new BaseReadTsKvQuery(
                    key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), cmd.getAgg()
            );
            queriesKeys.put(query.getId(), query.getKey());
            return query;
        }).collect(Collectors.toList());
        if (cmd.isFetchLatestPreviousPoint()) {
            finalTsKvQueryList = new ArrayList<>(tsKvQueryList);
            finalTsKvQueryList.addAll(keys.stream().map(key -> {
                        @NotNull var query = new BaseReadTsKvQuery(
                                key, cmd.getStartTs() - TimeUnit.DAYS.toMillis(365), cmd.getStartTs(), cmd.getInterval(), 1, cmd.getAgg());
                        queriesKeys.put(query.getId(), query.getKey());
                        return query;
                    }
            ).collect(Collectors.toList()));
        } else {
            finalTsKvQueryList = tsKvQueryList;
        }
        @NotNull Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), finalTsKvQueryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            @NotNull Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    @NotNull Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (@NotNull ReadTsKvQueryResult queryResult : queryResults) {
                            String queryKey = queriesKeys.get(queryResult.getQueryId());
                            if (queryKey != null) {
                                entityData.getTimeseries().merge(queryKey, queryResult.toTsValues(), ArrayUtils::addAll);
                                lastTsMap.merge(queryKey, queryResult.getLastEntryTs(), Math::max);
                            } else {
                                log.warn("ReadTsKvQueryResult for {} {} has queryId not matching the initial query",
                                        entityData.getEntityId().getEntityType(), entityData.getEntityId());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        if (!entityData.getTimeseries().containsKey(key)) {
                            entityData.getTimeseries().put(key, new TsValue[0]);
                        }
                    });

                    if (cmd.isFetchLatestPreviousPoint()) {
                        entityData.getTimeseries().values().forEach(dataArray -> Arrays.sort(dataArray, (o1, o2) -> Long.compare(o2.getTs(), o1.getTs())));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, cmd.getStartTs(), cmd.getEndTs());
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    private void handleLatestCmd(@NotNull TbEntityDataSubCtx ctx, @NotNull LatestValueCmd latestCmd) {
        log.trace("[{}][{}] Going to process latest command: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
        //Fetch the latest values for telemetry keys (in case they are not copied from NoSQL to SQL DB in hybrid mode.
        if (!tsInSqlDB) {
            log.trace("[{}][{}] Going to fetch missing latest values: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
            @NotNull List<String> allTsKeys = latestCmd.getKeys().stream()
                                                       .filter(key -> key.getType().equals(EntityKeyType.TIME_SERIES))
                                                       .map(EntityKey::getKey).collect(Collectors.toList());

            @NotNull Map<EntityData, ListenableFuture<Map<String, TsValue>>> missingTelemetryFutures = new HashMap<>();
            for (@NotNull EntityData entityData : ctx.getData().getData()) {
                Map<EntityKeyType, Map<String, TsValue>> latestEntityData = entityData.getLatest();
                Map<String, TsValue> tsEntityData = latestEntityData.get(EntityKeyType.TIME_SERIES);
                @NotNull Set<String> missingTsKeys = new LinkedHashSet<>(allTsKeys);
                if (tsEntityData != null) {
                    missingTsKeys.removeAll(tsEntityData.keySet());
                } else {
                    tsEntityData = new HashMap<>();
                    latestEntityData.put(EntityKeyType.TIME_SERIES, tsEntityData);
                }

                ListenableFuture<List<TsKvEntry>> missingTsData = tsService.findLatest(ctx.getTenantId(), entityData.getEntityId(), missingTsKeys);
                missingTelemetryFutures.put(entityData, Futures.transform(missingTsData, this::toTsValue, MoreExecutors.directExecutor()));
            }
            Futures.addCallback(Futures.allAsList(missingTelemetryFutures.values()), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<Map<String, TsValue>> result) {
                    missingTelemetryFutures.forEach((key, value) -> {
                        try {
                            key.getLatest().get(EntityKeyType.TIME_SERIES).putAll(value.get());
                        } catch (InterruptedException | ExecutionException e) {
                            log.warn("[{}][{}] Failed to lookup latest telemetry: {}:{}", ctx.getSessionId(), ctx.getCmdId(), key.getEntityId(), allTsKeys, e);
                        }
                    });
                    EntityDataUpdate update;
                    ctx.getWsLock().lock();
                    try {
                        ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                        if (!ctx.isInitialDataSent()) {
                            update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                            ctx.setInitialDataSent(true);
                        } else {
                            update = new EntityDataUpdate(ctx.getCmdId(), null, ctx.getData().getData(), ctx.getMaxEntitiesPerDataSubscription());
                        }
                        ctx.sendWsMsg(update);
                    } finally {
                        ctx.getWsLock().unlock();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process websocket command: {}:{}", ctx.getSessionId(), ctx.getCmdId(), ctx.getQuery(), latestCmd, t);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to process websocket command!"));
                }
            }, wsCallBackExecutor);
        } else {
            ctx.getWsLock().lock();
            try {
                ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                checkAndSendInitialData(ctx);
            } finally {
                ctx.getWsLock().unlock();
            }
        }
    }

    @NotNull
    private Map<String, TsValue> toTsValue(@NotNull List<TsKvEntry> data) {
        return data.stream().collect(Collectors.toMap(TsKvEntry::getKey, value -> new TsValue(value.getTs(), value.getValueAsString())));
    }

    @Override
    public void cancelSubscription(String sessionId, @NotNull UnsubscribeCmd cmd) {
        cleanupAndCancel(getSubCtx(sessionId, cmd.getCmdId()));
    }

    private void cleanupAndCancel(@org.jetbrains.annotations.Nullable TbAbstractSubCtx ctx) {
        if (ctx != null) {
            ctx.stop();
            if (ctx.getSessionId() != null) {
                Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(ctx.getSessionId());
                if (sessionSubs != null) {
                    sessionSubs.remove(ctx.getCmdId());
                }
            }
        }
    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.remove(sessionId);
        if (sessionSubs != null) {
            sessionSubs.values().forEach(this::cleanupAndCancel);
        }
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}
