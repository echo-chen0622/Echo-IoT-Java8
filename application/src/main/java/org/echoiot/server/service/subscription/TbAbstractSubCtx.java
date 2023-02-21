package org.echoiot.server.service.subscription;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.echoiot.server.service.telemetry.cmd.v2.CmdUpdate;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public abstract class TbAbstractSubCtx<T extends EntityCountQuery> {

    @Getter
    protected final Lock wsLock = new ReentrantLock(true);
    protected final String serviceId;
    protected final SubscriptionServiceStatistics stats;
    private final TelemetryWebSocketService wsService;
    protected final EntityService entityService;
    protected final TbLocalSubscriptionService localSubscriptionService;
    protected final AttributesService attributesService;
    protected final TelemetryWebSocketSessionRef sessionRef;
    protected final int cmdId;
    @NotNull
    protected final Set<Integer> subToDynamicValueKeySet;
    @NotNull
    @Getter
    protected final Map<DynamicValueKey, List<DynamicValue>> dynamicValues;
    @Getter
    @Setter
    protected T query;
    @Setter
    protected volatile ScheduledFuture<?> refreshTask;
    protected volatile boolean stopped;

    public TbAbstractSubCtx(String serviceId, TelemetryWebSocketService wsService,
                            EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                            AttributesService attributesService, SubscriptionServiceStatistics stats,
                            TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        this.serviceId = serviceId;
        this.wsService = wsService;
        this.entityService = entityService;
        this.localSubscriptionService = localSubscriptionService;
        this.attributesService = attributesService;
        this.stats = stats;
        this.sessionRef = sessionRef;
        this.cmdId = cmdId;
        this.subToDynamicValueKeySet = ConcurrentHashMap.newKeySet();
        this.dynamicValues = new ConcurrentHashMap<>();
    }

    public void setAndResolveQuery(@Nullable T query) {
        dynamicValues.clear();
        this.query = query;
        if (query != null && query.getKeyFilters() != null) {
            for (@NotNull KeyFilter filter : query.getKeyFilters()) {
                registerDynamicValues(filter.getPredicate());
            }
        }
        resolve(getTenantId(), getCustomerId(), getUserId());
    }

    public void resolve(TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId) {
        @NotNull List<ListenableFuture<DynamicValueKeySub>> futures = new ArrayList<>();
        for (@NotNull DynamicValueKey key : dynamicValues.keySet()) {
            switch (key.getSourceType()) {
                case CURRENT_TENANT:
                    futures.add(resolveEntityValue(tenantId, tenantId, key));
                    break;
                case CURRENT_CUSTOMER:
                    if (customerId != null && !customerId.isNullUid()) {
                        futures.add(resolveEntityValue(tenantId, customerId, key));
                    }
                    break;
                case CURRENT_USER:
                    if (userId != null && !userId.isNullUid()) {
                        futures.add(resolveEntityValue(tenantId, userId, key));
                    }
                    break;
            }
        }
        try {
            @NotNull Map<EntityId, Map<String, DynamicValueKeySub>> tmpSubMap = new HashMap<>();
            for (@NotNull DynamicValueKeySub sub : Futures.successfulAsList(futures).get()) {
                tmpSubMap.computeIfAbsent(sub.getEntityId(), tmp -> new HashMap<>()).put(sub.getKey().getSourceAttribute(), sub);
            }
            for (EntityId entityId : tmpSubMap.keySet()) {
                @NotNull Map<String, Long> keyStates = new HashMap<>();
                Map<String, DynamicValueKeySub> dynamicValueKeySubMap = tmpSubMap.get(entityId);
                dynamicValueKeySubMap.forEach((k, v) -> keyStates.put(k, v.getLastUpdateTs()));
                int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
                TbAttributeSubscription sub = TbAttributeSubscription.builder()
                        .serviceId(serviceId)
                        .sessionId(sessionRef.getSessionId())
                        .subscriptionId(subIdx)
                        .tenantId(sessionRef.getSecurityCtx().getTenantId())
                        .entityId(entityId)
                        .updateConsumer((s, subscriptionUpdate) -> dynamicValueSubUpdate(s, subscriptionUpdate, dynamicValueKeySubMap))
                        .allKeys(false)
                        .keyStates(keyStates)
                        .scope(TbAttributeSubscriptionScope.SERVER_SCOPE)
                        .build();
                subToDynamicValueKeySet.add(subIdx);
                localSubscriptionService.addSubscription(sub);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.info("[{}][{}][{}] Failed to resolve dynamic values: {}", tenantId, customerId, userId, dynamicValues.keySet());
        }

    }

    private void dynamicValueSubUpdate(String sessionId, @NotNull TelemetrySubscriptionUpdate subscriptionUpdate,
                                       @NotNull Map<String, DynamicValueKeySub> dynamicValueKeySubMap) {
        @NotNull Map<String, TsValue> latestUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
        });

        boolean invalidateFilter = false;
        for (@NotNull Map.Entry<String, TsValue> entry : latestUpdate.entrySet()) {
            String k = entry.getKey();
            TsValue tsValue = entry.getValue();
            DynamicValueKeySub sub = dynamicValueKeySubMap.get(k);
            if (sub.updateValue(tsValue)) {
                invalidateFilter = true;
                updateDynamicValuesByKey(sub, tsValue);
            }
        }

        if (invalidateFilter) {
            update();
        }
    }

    public abstract boolean isDynamic();

    public abstract void fetchData();

    protected abstract void update();

    public void clearSubscriptions() {
        clearDynamicValueSubscriptions();
    }

    public void stop() {
        stopped = true;
        cancelTasks();
        clearSubscriptions();
    }

    @Data
    private static class DynamicValueKeySub {
        @NotNull
        private final DynamicValueKey key;
        @NotNull
        private final EntityId entityId;
        private long lastUpdateTs;
        private String lastUpdateValue;

        boolean updateValue(@NotNull TsValue value) {
            if (value.getTs() > lastUpdateTs && (lastUpdateValue == null || !lastUpdateValue.equals(value.getValue()))) {
                this.lastUpdateTs = value.getTs();
                this.lastUpdateValue = value.getValue();
                return true;
            } else {
                return false;
            }
        }
    }

    @NotNull
    private ListenableFuture<DynamicValueKeySub> resolveEntityValue(TenantId tenantId, EntityId entityId, @NotNull DynamicValueKey key) {
        ListenableFuture<Optional<AttributeKvEntry>> entry = attributesService.find(tenantId, entityId,
                                                                                    TbAttributeSubscriptionScope.SERVER_SCOPE.name(), key.getSourceAttribute());
        return Futures.transform(entry, attributeOpt -> {
            @NotNull DynamicValueKeySub sub = new DynamicValueKeySub(key, entityId);
            if (attributeOpt.isPresent()) {
                @NotNull AttributeKvEntry attribute = attributeOpt.get();
                sub.setLastUpdateTs(attribute.getLastUpdateTs());
                sub.setLastUpdateValue(attribute.getValueAsString());
                updateDynamicValuesByKey(sub, new TsValue(attribute.getLastUpdateTs(), attribute.getValueAsString()));
            }
            return sub;
        }, MoreExecutors.directExecutor());
    }

    @SuppressWarnings("unchecked")
    protected void updateDynamicValuesByKey(@NotNull DynamicValueKeySub sub, @NotNull TsValue tsValue) {
        DynamicValueKey dvk = sub.getKey();
        switch (dvk.getPredicateType()) {
            case STRING:
                dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(tsValue.getValue()));
                break;
            case NUMERIC:
                try {
                    @NotNull Double dValue = Double.parseDouble(tsValue.getValue());
                    dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(dValue));
                } catch (NumberFormatException e) {
                    dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(null));
                }
                break;
            case BOOLEAN:
                @NotNull Boolean bValue = Boolean.parseBoolean(tsValue.getValue());
                dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(bValue));
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void registerDynamicValues(@NotNull KeyFilterPredicate predicate) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                @NotNull Optional<DynamicValue> value = getDynamicValueFromSimplePredicate((SimpleKeyFilterPredicate) predicate);
                if (value.isPresent()) {
                    @NotNull DynamicValue dynamicValue = value.get();
                    @NotNull DynamicValueKey key = new DynamicValueKey(
                            predicate.getType(),
                            dynamicValue.getSourceType(),
                            dynamicValue.getSourceAttribute());
                    dynamicValues.computeIfAbsent(key, tmp -> new ArrayList<>()).add(dynamicValue);
                }
                break;
            case COMPLEX:
                ((ComplexFilterPredicate) predicate).getPredicates().forEach(this::registerDynamicValues);
        }
    }

    @NotNull
    private Optional<DynamicValue<T>> getDynamicValueFromSimplePredicate(@NotNull SimpleKeyFilterPredicate<T> predicate) {
        if (predicate.getValue().getUserValue() == null) {
            return Optional.ofNullable(predicate.getValue().getDynamicValue());
        } else {
            return Optional.empty();
        }
    }

    public String getSessionId() {
        return sessionRef.getSessionId();
    }

    public TenantId getTenantId() {
        return sessionRef.getSecurityCtx().getTenantId();
    }

    public CustomerId getCustomerId() {
        return sessionRef.getSecurityCtx().getCustomerId();
    }

    public UserId getUserId() {
        return sessionRef.getSecurityCtx().getId();
    }

    protected void clearDynamicValueSubscriptions() {
        if (subToDynamicValueKeySet != null) {
            for (Integer subId : subToDynamicValueKeySet) {
                localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), subId);
            }
            subToDynamicValueKeySet.clear();
        }
    }

    public void setRefreshTask(@NotNull ScheduledFuture<?> task) {
        if (!stopped) {
            this.refreshTask = task;
        } else {
            task.cancel(true);
        }
    }

    public void cancelTasks() {
        if (this.refreshTask != null) {
            log.trace("[{}][{}] Canceling old refresh task", sessionRef.getSessionId(), cmdId);
            this.refreshTask.cancel(true);
        }
    }

    @Data
    public static class DynamicValueKey {
        @NotNull
        @Getter
        private final FilterPredicateType predicateType;
        @NotNull
        @Getter
        private final DynamicValueSourceType sourceType;
        @NotNull
        @Getter
        private final String sourceAttribute;
    }

    public void sendWsMsg(CmdUpdate update) {
        wsLock.lock();
        try {
            wsService.sendWsMsg(sessionRef.getSessionId(), update);
        } finally {
            wsLock.unlock();
        }
    }

}
