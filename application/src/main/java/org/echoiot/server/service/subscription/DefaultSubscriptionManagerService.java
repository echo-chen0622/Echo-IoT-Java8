package org.echoiot.server.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.DonAsynchron;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.state.DefaultDeviceStateService;
import org.echoiot.server.service.state.DeviceStateService;
import org.echoiot.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@TbCoreComponent
@Service
public class DefaultSubscriptionManagerService extends TbApplicationEventListener<PartitionChangeEvent> implements SubscriptionManagerService {

    @Resource
    private AttributesService attrService;

    @Resource
    private TimeseriesService tsService;

    @Resource
    private NotificationsTopicService notificationsTopicService;

    @Resource
    private PartitionService partitionService;

    @Resource
    private TbServiceInfoProvider serviceInfoProvider;

    @Resource
    private TbQueueProducerProvider producerProvider;

    @Resource
    private TbLocalSubscriptionService localSubscriptionService;

    @Resource
    private DeviceStateService deviceStateService;

    @Resource
    private TbClusterService clusterService;

    private final Map<EntityId, Set<TbSubscription>> subscriptionsByEntityId = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, TbSubscription>> subscriptionsByWsSessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<TopicPartitionInfo, Set<TbSubscription>> partitionedSubscriptions = new ConcurrentHashMap<>();
    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    private ExecutorService tsCallBackExecutor;
    private String serviceId;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNotificationsProducer;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("ts-sub-callback"));
        serviceId = serviceInfoProvider.getServiceId();
        toCoreNotificationsProducer = producerProvider.getTbCoreNotificationsMsgProducer();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void addSubscription(TbSubscription subscription, TbCallback callback) {
        log.trace("[{}][{}][{}] Registering subscription for entity [{}]",
                subscription.getServiceId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, subscription.getTenantId(), subscription.getEntityId());
        if (currentPartitions.contains(tpi)) {
            partitionedSubscriptions.computeIfAbsent(tpi, k -> ConcurrentHashMap.newKeySet()).add(subscription);
            callback.onSuccess();
        } else {
            log.warn("[{}][{}] Entity belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , subscription.getTenantId(), subscription.getEntityId(), tpi.getFullTopicName());
            callback.onFailure(new RuntimeException("Entity belongs to external partition " + tpi.getFullTopicName() + "!"));
        }
        boolean newSubscription = subscriptionsByEntityId
                .computeIfAbsent(subscription.getEntityId(), k -> ConcurrentHashMap.newKeySet()).add(subscription);
        subscriptionsByWsSessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>()).put(subscription.getSubscriptionId(), subscription);
        if (newSubscription) {
            switch (subscription.getType()) {
                case TIMESERIES:
                    handleNewTelemetrySubscription((TbTimeseriesSubscription) subscription);
                    break;
                case ATTRIBUTES:
                    handleNewAttributeSubscription((TbAttributeSubscription) subscription);
                    break;
                case ALARMS:
                    handleNewAlarmsSubscription((TbAlarmsSubscription) subscription);
                    break;
            }
        }
    }

    @Override
    public void cancelSubscription(String sessionId, int subscriptionId, TbCallback callback) {
        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
        Map<Integer, TbSubscription> sessionSubscriptions = subscriptionsByWsSessionId.get(sessionId);
        if (sessionSubscriptions != null) {
            TbSubscription subscription = sessionSubscriptions.remove(subscriptionId);
            if (subscription != null) {
                removeSubscriptionFromEntityMap(subscription);
                removeSubscriptionFromPartitionMap(subscription);
                if (sessionSubscriptions.isEmpty()) {
                    subscriptionsByWsSessionId.remove(sessionId);
                }
            } else {
                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
        callback.onSuccess();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(currentPartitions);
            removedPartitions.removeAll(partitionChangeEvent.getPartitions());

            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());

            // We no longer manage current partition of devices;
            removedPartitions.forEach(partition -> {
                Set<TbSubscription> subs = partitionedSubscriptions.remove(partition);
                if (subs != null) {
                    subs.forEach(sub -> {
                        if (!serviceId.equals(sub.getServiceId())) {
                            removeSubscriptionFromEntityMap(sub);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback) {
        onLocalTelemetrySubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.TIMESERIES.equals(s.getType())) {
                        return (TbTimeseriesSubscription) s;
                    } else {
                        return null;
                    }
                }, s -> true, s -> {
                    @Nullable List<TsKvEntry> subscriptionUpdate = null;
                    for (TsKvEntry kv : ts) {
                        if ((s.isAllKeys() || s.getKeyStates().containsKey((kv.getKey())))) {
                            if (subscriptionUpdate == null) {
                                subscriptionUpdate = new ArrayList<>();
                            }
                            subscriptionUpdate.add(kv);
                        }
                    }
                    return subscriptionUpdate;
                }, true);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            updateDeviceInactivityTimeout(tenantId, entityId, ts);
        }
        callback.onSuccess();
    }

    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback) {
        onAttributesUpdate(tenantId, entityId, scope, attributes, true, callback);
    }

    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, TbCallback callback) {
        onLocalTelemetrySubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.ATTRIBUTES.equals(s.getType())) {
                        return (TbAttributeSubscription) s;
                    } else {
                        return null;
                    }
                },
                s -> (TbAttributeSubscriptionScope.ANY_SCOPE.equals(s.getScope()) || scope.equals(s.getScope().name())),
                s -> {
                    @Nullable List<TsKvEntry> subscriptionUpdate = null;
                    for (AttributeKvEntry kv : attributes) {
                        if (s.isAllKeys() || s.getKeyStates().containsKey(kv.getKey())) {
                            if (subscriptionUpdate == null) {
                                subscriptionUpdate = new ArrayList<>();
                            }
                            subscriptionUpdate.add(new BasicTsKvEntry(kv.getLastUpdateTs(), kv));
                        }
                    }
                    return subscriptionUpdate;
                }, true);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)) {
                updateDeviceInactivityTimeout(tenantId, entityId, attributes);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onUpdate(tenantId,
                                                                                           new DeviceId(entityId.getId()), DataConstants.SHARED_SCOPE, new ArrayList<>(attributes))
                        , null);
            }
        }
        callback.onSuccess();
    }

    private void updateDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<? extends KvEntry> kvEntries) {
        for (KvEntry kvEntry : kvEntries) {
            if (kvEntry.getKey().equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), getLongValue(kvEntry));
            }
        }
    }

    private void deleteDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<String> keys) {
        for (String key : keys) {
            if (key.equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), 0);
            }
        }
    }

    @Override
    public void onAlarmUpdate(TenantId tenantId, EntityId entityId, Alarm alarm, TbCallback callback) {
        onLocalAlarmSubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.ALARMS.equals(s.getType())) {
                        return (TbAlarmsSubscription) s;
                    } else {
                        return null;
                    }
                },
                s -> alarm.getCreatedTime() >= s.getTs(),
                s -> alarm,
                false
        );
        callback.onSuccess();
    }

    @Override
    public void onAlarmDeleted(TenantId tenantId, EntityId entityId, Alarm alarm, TbCallback callback) {
        onLocalAlarmSubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.ALARMS.equals(s.getType())) {
                        return (TbAlarmsSubscription) s;
                    } else {
                        return null;
                    }
                },
                s -> alarm.getCreatedTime() >= s.getTs(),
                s -> alarm,
                true
        );
        callback.onSuccess();
    }

    @Override
    public void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, TbCallback callback) {
        onLocalTelemetrySubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.ATTRIBUTES.equals(s.getType())) {
                        return (TbAttributeSubscription) s;
                    } else {
                        return null;
                    }
                },
                s -> (TbAttributeSubscriptionScope.ANY_SCOPE.equals(s.getScope()) || scope.equals(s.getScope().name())),
                s -> {
                    @Nullable List<TsKvEntry> subscriptionUpdate = null;
                    for (String key : keys) {
                        if (s.isAllKeys() || s.getKeyStates().containsKey(key)) {
                            if (subscriptionUpdate == null) {
                                subscriptionUpdate = new ArrayList<>();
                            }
                            subscriptionUpdate.add(new BasicTsKvEntry(0, new StringDataEntry(key, "")));
                        }
                    }
                    return subscriptionUpdate;
                }, false);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)
                    || TbAttributeSubscriptionScope.ANY_SCOPE.name().equalsIgnoreCase(scope)) {
                deleteDeviceInactivityTimeout(tenantId, entityId, keys);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(tenantId,
                                new DeviceId(entityId.getId()), scope, keys), null);
            }
        }
        callback.onSuccess();
    }

    @Override
    public void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback) {
        onLocalTelemetrySubUpdate(entityId,
                s -> {
                    if (TbSubscriptionType.TIMESERIES.equals(s.getType())) {
                        return (TbTimeseriesSubscription) s;
                    } else {
                        return null;
                    }
                }, s -> true, s -> {
                    @Nullable List<TsKvEntry> subscriptionUpdate = null;
                    for (String key : keys) {
                        if (s.isAllKeys() || s.getKeyStates().containsKey(key)) {
                            if (subscriptionUpdate == null) {
                                subscriptionUpdate = new ArrayList<>();
                            }
                            subscriptionUpdate.add(new BasicTsKvEntry(0, new StringDataEntry(key, "")));
                        }
                    }
                    return subscriptionUpdate;
                }, false);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            deleteDeviceInactivityTimeout(tenantId, entityId, keys);
        }
        callback.onSuccess();
    }

    private <T extends TbSubscription> void onLocalTelemetrySubUpdate(EntityId entityId,
                                                                      Function<TbSubscription, T> castFunction,
                                                                      Predicate<T> filterFunction,
                                                                      Function<T, List<TsKvEntry>> processFunction,
                                                                      boolean ignoreEmptyUpdates) {
        Set<TbSubscription> entitySubscriptions = subscriptionsByEntityId.get(entityId);
        if (entitySubscriptions != null) {
            entitySubscriptions.stream().map(castFunction).filter(Objects::nonNull).filter(filterFunction).forEach(s -> {
                List<TsKvEntry> subscriptionUpdate = processFunction.apply(s);
                if (subscriptionUpdate != null && !subscriptionUpdate.isEmpty()) {
                    if (serviceId.equals(s.getServiceId())) {
                        TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(s.getSubscriptionId(), subscriptionUpdate);
                        localSubscriptionService.onSubscriptionUpdate(s.getSessionId(), update, TbCallback.EMPTY);
                    } else {
                        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, s.getServiceId());
                        toCoreNotificationsProducer.send(tpi, toProto(s, subscriptionUpdate, ignoreEmptyUpdates), null);
                    }
                }
            });
        } else {
            log.debug("[{}] No device subscriptions to process!", entityId);
        }
    }

    private void onLocalAlarmSubUpdate(EntityId entityId,
                                       Function<TbSubscription, TbAlarmsSubscription> castFunction,
                                       Predicate<TbAlarmsSubscription> filterFunction,
                                       Function<TbAlarmsSubscription, Alarm> processFunction, boolean deleted) {
        Set<TbSubscription> entitySubscriptions = subscriptionsByEntityId.get(entityId);
        if (entitySubscriptions != null) {
            entitySubscriptions.stream().map(castFunction).filter(Objects::nonNull).filter(filterFunction).forEach(s -> {
                Alarm alarm = processFunction.apply(s);
                if (alarm != null) {
                    if (serviceId.equals(s.getServiceId())) {
                        AlarmSubscriptionUpdate update = new AlarmSubscriptionUpdate(s.getSubscriptionId(), alarm, deleted);
                        localSubscriptionService.onSubscriptionUpdate(s.getSessionId(), update, TbCallback.EMPTY);
                    } else {
                        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, s.getServiceId());
                        toCoreNotificationsProducer.send(tpi, toProto(s, alarm, deleted), null);
                    }
                }
            });
        } else {
            log.debug("[{}] No device subscriptions to process!", entityId);
        }
    }

    private void removeSubscriptionFromEntityMap(TbSubscription sub) {
        Set<TbSubscription> entitySubSet = subscriptionsByEntityId.get(sub.getEntityId());
        if (entitySubSet != null) {
            entitySubSet.remove(sub);
            if (entitySubSet.isEmpty()) {
                subscriptionsByEntityId.remove(sub.getEntityId());
            }
        }
    }

    private void removeSubscriptionFromPartitionMap(TbSubscription sub) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId());
        Set<TbSubscription> subs = partitionedSubscriptions.get(tpi);
        if (subs != null) {
            subs.remove(sub);
        }
    }

    private void handleNewAttributeSubscription(TbAttributeSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote attribute subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());

        final Map<String, Long> keyStates = subscription.getKeyStates();
        DonAsynchron.withCallback(attrService.find(subscription.getTenantId(), subscription.getEntityId(), DataConstants.CLIENT_SCOPE, keyStates.keySet()), values -> {
                    List<TsKvEntry> missedUpdates = new ArrayList<>();
                    values.forEach(latestEntry -> {
                        if (latestEntry.getLastUpdateTs() > keyStates.get(latestEntry.getKey())) {
                            missedUpdates.add(new BasicTsKvEntry(latestEntry.getLastUpdateTs(), latestEntry));
                        }
                    });
                    if (!missedUpdates.isEmpty()) {
                        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                        toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
                    }
                },
                e -> log.error("Failed to fetch missed updates.", e), tsCallBackExecutor);
    }

    private void handleNewAlarmsSubscription(TbAlarmsSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote alarm subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        //TODO: @dlandiak search all new alarms for this entity.
    }

    private void handleNewTelemetrySubscription(TbTimeseriesSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote telemetry subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());

        long curTs = System.currentTimeMillis();

        if (subscription.isLatestValues()) {
            DonAsynchron.withCallback(tsService.findLatest(subscription.getTenantId(), subscription.getEntityId(), subscription.getKeyStates().keySet()),
                    missedUpdates -> {
                        if (missedUpdates != null && !missedUpdates.isEmpty()) {
                            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                            toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
                        }
                    },
                    e -> log.error("Failed to fetch missed updates.", e),
                    tsCallBackExecutor);
        } else {
            List<ReadTsKvQuery> queries = new ArrayList<>();
            subscription.getKeyStates().forEach((key, value) -> {
                if (curTs > value) {
                    long startTs = subscription.getStartTime() > 0 ? Math.max(subscription.getStartTime(), value + 1L) : (value + 1L);
                    long endTs = subscription.getEndTime() > 0 ? Math.min(subscription.getEndTime(), curTs) : curTs;
                    queries.add(new BaseReadTsKvQuery(key, startTs, endTs, 0, 1000, Aggregation.NONE));
                }
            });
            if (!queries.isEmpty()) {
                DonAsynchron.withCallback(tsService.findAll(subscription.getTenantId(), subscription.getEntityId(), queries),
                        missedUpdates -> {
                            if (missedUpdates != null && !missedUpdates.isEmpty()) {
                                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                                toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
                            }
                        },
                        e -> log.error("Failed to fetch missed updates.", e),
                        tsCallBackExecutor);
            }
        }
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, List<TsKvEntry> updates) {
        return toProto(subscription, updates, true);
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, List<TsKvEntry> updates, boolean ignoreEmptyUpdates) {
        TbSubscriptionUpdateProto.Builder builder = TbSubscriptionUpdateProto.newBuilder();

        builder.setSessionId(subscription.getSessionId());
        builder.setSubscriptionId(subscription.getSubscriptionId());

        Map<String, List<Object>> data = new TreeMap<>();
        for (TsKvEntry tsEntry : updates) {
            List<Object> values = data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>());
            Object[] value = new Object[2];
            value[0] = tsEntry.getTs();
            value[1] = tsEntry.getValueAsString();
            values.add(value);
        }

        data.forEach((key, value) -> {
            TbSubscriptionUpdateValueListProto.Builder dataBuilder = TbSubscriptionUpdateValueListProto.newBuilder();
            dataBuilder.setKey(key);
            boolean hasData = false;
            for (Object v : value) {
                Object[] array = (Object[]) v;
                TbSubscriptionUpdateTsValue.Builder tsValueBuilder = TbSubscriptionUpdateTsValue.newBuilder();
                tsValueBuilder.setTs((long) array[0]);
                String strVal = (String) array[1];
                if (strVal != null) {
                    hasData = true;
                    tsValueBuilder.setValue(strVal);
                }
                dataBuilder.addTsValue(tsValueBuilder.build());
            }
            if (!ignoreEmptyUpdates || hasData) {
                builder.addData(dataBuilder.build());
            }
        });

        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToLocalSubscriptionServiceMsg(
                        LocalSubscriptionServiceMsgProto.newBuilder().setSubUpdate(builder.build()).build())
                .build();
        return new TbProtoQueueMsg<>(subscription.getEntityId().getId(), toCoreMsg);
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, Alarm alarm, boolean deleted) {
        TbAlarmSubscriptionUpdateProto.Builder builder = TbAlarmSubscriptionUpdateProto.newBuilder();

        builder.setSessionId(subscription.getSessionId());
        builder.setSubscriptionId(subscription.getSubscriptionId());
        builder.setAlarm(JacksonUtil.toString(alarm));
        builder.setDeleted(deleted);

        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToLocalSubscriptionServiceMsg(
                        LocalSubscriptionServiceMsgProto.newBuilder()
                                .setAlarmSubUpdate(builder.build()).build())
                .build();
        return new TbProtoQueueMsg<>(subscription.getEntityId().getId(), toCoreMsg);
    }

    private static long getLongValue(KvEntry kve) {
        switch (kve.getDataType()) {
            case LONG:
                return kve.getLongValue().orElse(0L);
            case DOUBLE:
                return kve.getDoubleValue().orElse(0.0).longValue();
            case STRING:
                try {
                    return Long.parseLong(kve.getStrValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            case JSON:
                try {
                    return Long.parseLong(kve.getJsonValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            default:
                return 0L;
        }
    }

}
