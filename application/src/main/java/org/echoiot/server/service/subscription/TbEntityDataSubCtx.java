package org.echoiot.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.kv.Aggregation;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.echoiot.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.echoiot.server.service.telemetry.cmd.v2.TimeSeriesCmd;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TbEntityDataSubCtx extends TbAbstractDataSubCtx<EntityDataQuery> {

    @Getter
    @Setter
    private volatile boolean initialDataSent;
    private TimeSeriesCmd curTsCmd;
    private LatestValueCmd latestValueCmd;
    @Getter
    private final int maxEntitiesPerDataSubscription;
    private Map<EntityId, Map<String, TsValue>> latestTsEntityData;

    public TbEntityDataSubCtx(String serviceId, TelemetryWebSocketService wsService, EntityService entityService,
                              TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                              SubscriptionServiceStatistics stats, TelemetryWebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerDataSubscription) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.maxEntitiesPerDataSubscription = maxEntitiesPerDataSubscription;
    }

    @Override
    public void fetchData() {
        super.fetchData();
        this.updateLatestTsData(this.data);
    }

    @Override
    protected void sendWsMsg(String sessionId, @NotNull TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            log.trace("[{}][{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
            if (resultToLatestValues) {
                sendLatestWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
            } else {
                sendTsWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
            }
        } else {
            log.trace("[{}][{}][{}][{}] Received stale subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
        }
    }

    @NotNull
    @Override
    protected Aggregation getCurrentAggregation() {
        return (this.curTsCmd == null || this.curTsCmd.getAgg() == null) ? Aggregation.NONE : this.curTsCmd.getAgg();
    }

    private void sendLatestWsMsg(EntityId entityId, String sessionId, @NotNull TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        @NotNull Map<String, TsValue> latestUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
        });
        @Nullable EntityData entityData = getDataForEntity(entityId);
        if (entityData != null && entityData.getLatest() != null) {
            Map<String, TsValue> latestCtxValues = entityData.getLatest().get(keyType);
            log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
            if (latestCtxValues != null) {
                latestCtxValues.forEach((k, v) -> {
                    TsValue update = latestUpdate.get(k);
                    if (update != null) {
                        //Ignore notifications about deleted keys
                        if (!(update.getTs() == 0 && (update.getValue() == null || update.getValue().isEmpty()))) {
                            if (update.getTs() < v.getTs()) {
                                log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                                log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            }
                        } else {
                            log.trace("[{}][{}][{}] Received deleted notification for: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k);
                        }
                    }
                });
                //Setting new values
                latestUpdate.forEach(latestCtxValues::put);
            }
        }
        if (!latestUpdate.isEmpty()) {
            @NotNull Map<EntityKeyType, Map<String, TsValue>> latestMap = Collections.singletonMap(keyType, latestUpdate);
            entityData = new EntityData(entityId, latestMap, null);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    private void sendTsWsMsg(EntityId entityId, String sessionId, @NotNull TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        @NotNull Map<String, List<TsValue>> tsUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            tsUpdate.computeIfAbsent(k, key -> new ArrayList<>()).add(new TsValue((Long) data[0], (String) data[1]));
        });
        Map<String, TsValue> latestCtxValues = getLatestTsValuesForEntity(entityId);
        log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
        if (latestCtxValues != null) {
            latestCtxValues.forEach((k, v) -> {
                List<TsValue> updateList = tsUpdate.get(k);
                if (updateList != null) {
                    for (@NotNull TsValue update : new ArrayList<>(updateList)) {
                        if (update.getTs() < v.getTs()) {
                            log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            // Looks like this is redundant feature and our UI is ready to merge the updates.
                            //updateList.remove(update);
                        } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                            log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            updateList.remove(update);
                        }
                        if (updateList.isEmpty()) {
                            tsUpdate.remove(k);
                        }
                    }
                }
            });
            //Setting new values
            tsUpdate.forEach((k, v) -> {
                @NotNull Optional<TsValue> maxValue = v.stream().max(Comparator.comparingLong(TsValue::getTs));
                maxValue.ifPresent(max -> latestCtxValues.put(k, max));
            });
        }
        if (!tsUpdate.isEmpty()) {
            @NotNull Map<String, TsValue[]> tsMap = new HashMap<>();
            tsUpdate.forEach((key, tsValue) -> tsMap.put(key, tsValue.toArray(new TsValue[tsValue.size()])));
            @NotNull EntityData entityData = new EntityData(entityId, null, tsMap);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    @Nullable
    private EntityData getDataForEntity(EntityId entityId) {
        return data.getData().stream().filter(item -> item.getEntityId().equals(entityId)).findFirst().orElse(null);
    }

    private Map<String, TsValue> getLatestTsValuesForEntity(EntityId entityId) {
        return latestTsEntityData.get(entityId);
    }

    private void updateLatestTsData(@NotNull PageData<EntityData> data) {
        latestTsEntityData = new HashMap<>();
        data.getData().stream().forEach(entityData -> {
            @NotNull Map<String, TsValue> latestTsMap = new HashMap<>();
            latestTsEntityData.put(entityData.getEntityId(), latestTsMap);
            if (entityData.getLatest() != null) {
                Map<String, TsValue> latestTsValues = entityData.getLatest().get(EntityKeyType.TIME_SERIES);
                if (latestTsValues != null) {
                    latestTsValues.forEach(latestTsMap::put);
                }
            }
        });
    }

    @Override
    public synchronized void doUpdate(@NotNull Map<EntityId, EntityData> newDataMap) {
        this.updateLatestTsData(this.data);
        @NotNull List<Integer> subIdsToCancel = new ArrayList<>();
        @NotNull List<TbSubscription> subsToAdd = new ArrayList<>();
        @NotNull Set<EntityId> currentSubs = new HashSet<>();
        subToEntityIdMap.forEach((subId, entityId) -> {
            if (!newDataMap.containsKey(entityId)) {
                subIdsToCancel.add(subId);
            } else {
                currentSubs.add(entityId);
            }
        });
        log.trace("[{}][{}] Subscriptions that are invalid: {}", sessionRef.getSessionId(), cmdId, subIdsToCancel);
        subIdsToCancel.forEach(subToEntityIdMap::remove);
        @NotNull List<EntityData> newSubsList = newDataMap.entrySet().stream().filter(entry -> !currentSubs.contains(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        if (!newSubsList.isEmpty()) {
            // NOTE: We ignore the TS subscriptions for new entities here, because widgets will re-init it's content and will create new subscriptions.
            if (curTsCmd == null && latestValueCmd != null) {
                List<EntityKey> keys = latestValueCmd.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
                    newSubsList.forEach(
                            entity -> {
                                log.trace("[{}][{}] Found new subscription for entity: {}", sessionRef.getSessionId(), cmdId, entity.getEntityId());
                                subsToAdd.addAll(addSubscriptions(entity, keysByType, true, 0, 0));
                            }
                    );
                }
            }
        }
        subIdsToCancel.forEach(subId -> localSubscriptionService.cancelSubscription(getSessionId(), subId));
        subsToAdd.forEach(localSubscriptionService::addSubscription);
        sendWsMsg(new EntityDataUpdate(cmdId, data, null, maxEntitiesPerDataSubscription));
    }

    public void setCurrentCmd(@NotNull EntityDataCmd cmd) {
        curTsCmd = cmd.getTsCmd();
        latestValueCmd = cmd.getLatestCmd();
    }

    @Override
    protected EntityDataQuery buildEntityDataQuery() {
        return query;
    }
}
