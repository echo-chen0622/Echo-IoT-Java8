package org.echoiot.server.service.subscription;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.kv.Aggregation;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.echoiot.server.common.data.query.AbstractDataQuery;
import org.echoiot.server.common.data.query.EntityData;
import org.echoiot.server.common.data.query.EntityDataPageLink;
import org.echoiot.server.common.data.query.EntityDataQuery;
import org.echoiot.server.common.data.query.EntityKey;
import org.echoiot.server.common.data.query.EntityKeyType;
import org.echoiot.server.common.data.query.TsValue;
import org.echoiot.server.service.telemetry.TelemetryWebSocketService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class TbAbstractDataSubCtx<T extends AbstractDataQuery<? extends EntityDataPageLink>> extends TbAbstractSubCtx<T> {

    @NotNull
    protected final Map<Integer, EntityId> subToEntityIdMap;
    @Getter
    protected PageData<EntityData> data;

    public TbAbstractDataSubCtx(String serviceId, TelemetryWebSocketService wsService,
                                EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                                AttributesService attributesService, SubscriptionServiceStatistics stats,
                                TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.subToEntityIdMap = new ConcurrentHashMap<>();
    }

    @Override
    public void fetchData() {
        this.data = findEntityData();
    }

    protected PageData<EntityData> findEntityData() {
        PageData<EntityData> result = entityService.findEntityDataByQuery(getTenantId(), getCustomerId(), buildEntityDataQuery());
        if (log.isTraceEnabled()) {
            result.getData().forEach(ed -> {
                log.trace("[{}][{}] EntityData: {}", getSessionId(), getCmdId(), ed);
            });
        }
        return result;
    }

    @Override
    public boolean isDynamic() {
        return query != null && query.getPageLink().isDynamic();
    }

    @Override
    protected synchronized void update() {
        PageData<EntityData> newData = findEntityData();
        Map<EntityId, EntityData> oldDataMap;
        if (data != null && !data.getData().isEmpty()) {
            oldDataMap = data.getData().stream().collect(Collectors.toMap(EntityData::getEntityId, Function.identity(), (a, b) -> a));
        } else {
            oldDataMap = Collections.emptyMap();
        }
        @NotNull Map<EntityId, EntityData> newDataMap = newData.getData().stream().collect(Collectors.toMap(EntityData::getEntityId, Function.identity(), (a, b) -> a));
        if (oldDataMap.size() == newDataMap.size() && oldDataMap.keySet().equals(newDataMap.keySet())) {
            log.trace("[{}][{}] No updates to entity data found", sessionRef.getSessionId(), cmdId);
        } else {
            this.data = newData;
            doUpdate(newDataMap);
        }
    }

    protected abstract void doUpdate(Map<EntityId, EntityData> newDataMap);

    protected abstract EntityDataQuery buildEntityDataQuery();

    public List<EntityData> getEntitiesData() {
        return data.getData();
    }

    @Override
    public void clearSubscriptions() {
        clearEntitySubscriptions();
        super.clearSubscriptions();
    }

    public void clearEntitySubscriptions() {
        if (subToEntityIdMap != null) {
            for (Integer subId : subToEntityIdMap.keySet()) {
                localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), subId);
            }
            subToEntityIdMap.clear();
        }
    }

    public void createLatestValuesSubscriptions(@NotNull List<EntityKey> keys) {
        createSubscriptions(keys, true, 0, 0);
    }

    public void createTimeSeriesSubscriptions(@NotNull Map<EntityData, Map<String, Long>> entityKeyStates, long startTs, long endTs) {
        createTimeSeriesSubscriptions(entityKeyStates, startTs, endTs, false);
    }

    public void createTimeSeriesSubscriptions(@NotNull Map<EntityData, Map<String, Long>> entityKeyStates, long startTs, long endTs, boolean resultToLatestValues) {
        entityKeyStates.forEach((entityData, keyStates) -> {
            int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
            subToEntityIdMap.put(subIdx, entityData.getEntityId());
            localSubscriptionService.addSubscription(
                    createTsSub(entityData, subIdx, false, startTs, endTs, keyStates, resultToLatestValues));
        });
    }

    private void createSubscriptions(@NotNull List<EntityKey> keys, boolean latestValues, long startTs, long endTs) {
        @NotNull Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
        for (@NotNull EntityData entityData : data.getData()) {
            @NotNull List<TbSubscription> entitySubscriptions = addSubscriptions(entityData, keysByType, latestValues, startTs, endTs);
            entitySubscriptions.forEach(localSubscriptionService::addSubscription);
        }
    }

    @NotNull
    protected Map<EntityKeyType, List<EntityKey>> getEntityKeyByTypeMap(@NotNull List<EntityKey> keys) {
        @NotNull Map<EntityKeyType, List<EntityKey>> keysByType = new HashMap<>();
        keys.forEach(key -> keysByType.computeIfAbsent(key.getType(), k -> new ArrayList<>()).add(key));
        return keysByType;
    }

    @NotNull
    protected List<TbSubscription> addSubscriptions(@NotNull EntityData entityData, @NotNull Map<EntityKeyType, List<EntityKey>> keysByType, boolean latestValues, long startTs, long endTs) {
        @NotNull List<TbSubscription> subscriptionList = new ArrayList<>();
        keysByType.forEach((keysType, keysList) -> {
            int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
            subToEntityIdMap.put(subIdx, entityData.getEntityId());
            switch (keysType) {
                case TIME_SERIES:
                    subscriptionList.add(createTsSub(entityData, subIdx, keysList, latestValues, startTs, endTs));
                    break;
                case CLIENT_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.CLIENT_SCOPE, keysList));
                    break;
                case SHARED_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.SHARED_SCOPE, keysList));
                    break;
                case SERVER_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.SERVER_SCOPE, keysList));
                    break;
                case ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.ANY_SCOPE, keysList));
                    break;
            }
        });
        return subscriptionList;
    }

    private TbSubscription createAttrSub(@NotNull EntityData entityData, int subIdx, EntityKeyType keysType, TbAttributeSubscriptionScope scope, @NotNull List<EntityKey> subKeys) {
        @NotNull Map<String, Long> keyStates = buildKeyStats(entityData, keysType, subKeys, true);
        log.trace("[{}][{}][{}] Creating attributes subscription for [{}] with keys: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), keyStates);
        return TbAttributeSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateConsumer((s, subscriptionUpdate) -> sendWsMsg(s, subscriptionUpdate, keysType))
                .allKeys(false)
                .keyStates(keyStates)
                .scope(scope)
                .build();
    }

    private TbSubscription createTsSub(@NotNull EntityData entityData, int subIdx, @NotNull List<EntityKey> subKeys, boolean latestValues, long startTs, long endTs) {
        @NotNull Map<String, Long> keyStates = buildKeyStats(entityData, EntityKeyType.TIME_SERIES, subKeys, latestValues);
        if (!latestValues && entityData.getTimeseries() != null) {
            entityData.getTimeseries().forEach((k, v) -> {
                long ts = Arrays.stream(v).map(TsValue::getTs).max(Long::compareTo).orElse(0L);
                log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, ts);
                if (!Aggregation.NONE.equals(getCurrentAggregation()) && ts < endTs) {
                    ts = endTs;
                }
                keyStates.put(k, ts);
            });
        }
        return createTsSub(entityData, subIdx, latestValues, startTs, endTs, keyStates);
    }

    private TbTimeseriesSubscription createTsSub(@NotNull EntityData entityData, int subIdx, boolean latestValues, long startTs, long endTs, Map<String, Long> keyStates) {
        return createTsSub(entityData, subIdx, latestValues, startTs, endTs, keyStates, latestValues);
    }

    private TbTimeseriesSubscription createTsSub(@NotNull EntityData entityData, int subIdx, boolean latestValues, long startTs, long endTs, Map<String, Long> keyStates, boolean resultToLatestValues) {
        log.trace("[{}][{}][{}] Creating time-series subscription for [{}] with keys: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), keyStates);
        return TbTimeseriesSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateConsumer((sessionId, subscriptionUpdate) -> sendWsMsg(sessionId, subscriptionUpdate, EntityKeyType.TIME_SERIES, resultToLatestValues))
                .allKeys(false)
                .keyStates(keyStates)
                .latestValues(latestValues)
                .startTime(startTs)
                .endTime(endTs)
                .build();
    }

    private void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        sendWsMsg(sessionId, subscriptionUpdate, keyType, true);
    }

    @NotNull
    private Map<String, Long> buildKeyStats(@NotNull EntityData entityData, EntityKeyType keysType, @NotNull List<EntityKey> subKeys, boolean latestValues) {
        @NotNull Map<String, Long> keyStates = new HashMap<>();
        subKeys.forEach(key -> keyStates.put(key.getKey(), 0L));
        if (latestValues && entityData.getLatest() != null) {
            Map<String, TsValue> currentValues = entityData.getLatest().get(keysType);
            if (currentValues != null) {
                currentValues.forEach((k, v) -> {
                    if (subKeys.contains(new EntityKey(keysType, k))) {
                        log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, v.getTs());
                        keyStates.put(k, v.getTs());
                    }
                });
            }
        }
        return keyStates;
    }

    abstract void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues);

    protected abstract Aggregation getCurrentAggregation();
}
