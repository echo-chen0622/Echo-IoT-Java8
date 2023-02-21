package org.echoiot.server.service.telemetry;

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.ApiUsageRecordKey;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.service.apiusage.TbApiUsageStateService;
import org.echoiot.server.service.entitiy.entityview.TbEntityViewService;
import org.echoiot.server.service.subscription.TbSubscriptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Echo on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService extends AbstractSubscriptionService implements TelemetrySubscriptionService {

    private final AttributesService attrService;
    private final TimeseriesService tsService;
    private final TbEntityViewService tbEntityViewService;
    private final TbApiUsageReportClient apiUsageClient;
    private final TbApiUsageStateService apiUsageStateService;

    private ExecutorService tsCallBackExecutor;

    public DefaultTelemetrySubscriptionService(AttributesService attrService,
                                               TimeseriesService tsService,
                                               @Lazy TbEntityViewService tbEntityViewService,
                                               TbClusterService clusterService,
                                               PartitionService partitionService,
                                               TbApiUsageReportClient apiUsageClient,
                                               TbApiUsageStateService apiUsageStateService) {
        super(clusterService, partitionService);
        this.attrService = attrService;
        this.tsService = tsService;
        this.tbEntityViewService = tbEntityViewService;
        this.apiUsageClient = apiUsageClient;
        this.apiUsageStateService = apiUsageStateService;
    }

    @PostConstruct
    public void initExecutor() {
        super.initExecutor();
        tsCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("ts-service-ts-callback"));
    }

    @NotNull
    @Override
    protected String getExecutorPrefix() {
        return "ts";
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
        super.shutdownExecutor();
    }

    @NotNull
    @Override
    public ListenableFuture<Void> saveAndNotify(TenantId tenantId, @NotNull EntityId entityId, TsKvEntry ts) {
        @NotNull SettableFuture<Void> future = SettableFuture.create();
        saveAndNotify(tenantId, entityId, Collections.singletonList(ts), new VoidFutureCallback(future));
        return future;
    }

    @Override
    public void saveAndNotify(TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, null, entityId, ts, 0L, callback);
    }

    @Override
    public void saveAndNotify(TenantId tenantId, CustomerId customerId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, long ttl, @NotNull FutureCallback<Void> callback) {
        doSaveAndNotify(tenantId, customerId, entityId, ts, ttl, callback, true);
    }

    @Override
    public void saveWithoutLatestAndNotify(TenantId tenantId, CustomerId customerId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, long ttl, @NotNull FutureCallback<Void> callback) {
        doSaveAndNotify(tenantId, customerId, entityId, ts, ttl, callback, false);
    }

    private void doSaveAndNotify(@org.jetbrains.annotations.Nullable TenantId tenantId, CustomerId customerId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, long ttl, @NotNull FutureCallback<Void> callback, boolean saveLatest) {
        checkInternalEntity(entityId);
        boolean sysTenant = TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId == null;
        if (sysTenant || apiUsageStateService.getApiUsageState(tenantId).isDbStorageEnabled()) {
            if (saveLatest) {
                saveAndNotifyInternal(tenantId, entityId, ts, ttl, getCallback(tenantId, customerId, sysTenant, callback));
            } else {
                saveWithoutLatestAndNotifyInternal(tenantId, entityId, ts, ttl, getCallback(tenantId, customerId, sysTenant, callback));
            }
        } else {
            callback.onFailure(new RuntimeException("DB storage writes are disabled due to API limits!"));
        }
    }

    @NotNull
    private FutureCallback<Integer> getCallback(TenantId tenantId, CustomerId customerId, boolean sysTenant, @NotNull FutureCallback<Void> callback) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(@org.jetbrains.annotations.Nullable Integer result) {
                if (!sysTenant && result != null && result > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.STORAGE_DP_COUNT, result);
                }
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    @Override
    public void saveAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, @NotNull FutureCallback<Integer> callback) {
        saveAndNotifyInternal(tenantId, entityId, ts, 0L, callback);
    }

    @Override
    public void saveAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, long ttl, @NotNull FutureCallback<Integer> callback) {
        ListenableFuture<Integer> saveFuture = tsService.save(tenantId, entityId, ts, ttl);
        addCallbacks(tenantId, entityId, ts, callback, saveFuture);
    }

    private void saveWithoutLatestAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, long ttl, @NotNull FutureCallback<Integer> callback) {
        ListenableFuture<Integer> saveFuture = tsService.saveWithoutLatest(tenantId, entityId, ts, ttl);
        addCallbacks(tenantId, entityId, ts, callback, saveFuture);
    }

    private void addCallbacks(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, @NotNull FutureCallback<Integer> callback, @NotNull ListenableFuture<Integer> saveFuture) {
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
        if (EntityType.DEVICE.equals(entityId.getEntityType()) || EntityType.ASSET.equals(entityId.getEntityType())) {
            Futures.addCallback(this.tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId),
                    new FutureCallback<List<EntityView>>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityView> result) {
                            if (result != null && !result.isEmpty()) {
                                @NotNull Map<String, List<TsKvEntry>> tsMap = new HashMap<>();
                                for (@NotNull TsKvEntry entry : ts) {
                                    tsMap.computeIfAbsent(entry.getKey(), s -> new ArrayList<>()).add(entry);
                                }
                                for (@NotNull EntityView entityView : result) {
                                    @NotNull List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                                            entityView.getKeys().getTimeseries() : new ArrayList<>(tsMap.keySet());
                                    @NotNull List<TsKvEntry> entityViewLatest = new ArrayList<>();
                                    long startTs = entityView.getStartTimeMs();
                                    long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
                                    for (String key : keys) {
                                        List<TsKvEntry> entries = tsMap.get(key);
                                        if (entries != null) {
                                            @NotNull Optional<TsKvEntry> tsKvEntry = entries.stream()
                                                                                            .filter(entry -> entry.getTs() > startTs && entry.getTs() <= endTs)
                                                                                            .max(Comparator.comparingLong(TsKvEntry::getTs));
                                            tsKvEntry.ifPresent(entityViewLatest::add);
                                        }
                                    }
                                    if (!entityViewLatest.isEmpty()) {
                                        saveLatestAndNotify(tenantId, entityView.getId(), entityViewLatest, new FutureCallback<Void>() {
                                            @Override
                                            public void onSuccess(@Nullable Void tmp) {
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                            }
                                        });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Error while finding entity views by tenantId and entityId", t);
                        }
                    }, MoreExecutors.directExecutor());
        }
    }

    @Override

    public void saveAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, @NotNull List<AttributeKvEntry> attributes, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, attributes, true, callback);
    }

    @Override
    public void saveAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, @NotNull List<AttributeKvEntry> attributes, boolean notifyDevice, @NotNull FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        saveAndNotifyInternal(tenantId, entityId, scope, attributes, notifyDevice, callback);
    }

    @Override
    public void saveAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, @NotNull List<AttributeKvEntry> attributes, boolean notifyDevice, @NotNull FutureCallback<Void> callback) {
        ListenableFuture<List<String>> saveFuture = attrService.save(tenantId, entityId, scope, attributes);
        addVoidCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onAttributesUpdate(tenantId, entityId, scope, attributes, notifyDevice));
    }

    @Override
    public void saveLatestAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, @NotNull FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        saveLatestAndNotifyInternal(tenantId, entityId, ts, callback);
    }

    @Override
    public void saveLatestAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts, @NotNull FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = tsService.saveLatest(tenantId, entityId, ts);
        addVoidCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
    }

    @Override
    public void deleteAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, List<String> keys, @NotNull FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteAndNotifyInternal(tenantId, entityId, scope, keys, false, callback);
    }

    @Override
    public void deleteAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, List<String> keys, boolean notifyDevice, @NotNull FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteAndNotifyInternal(tenantId, entityId, scope, keys, notifyDevice, callback);
    }

    @Override
    public void deleteAndNotifyInternal(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, List<String> keys, boolean notifyDevice, @NotNull FutureCallback<Void> callback) {
        ListenableFuture<List<String>> deleteFuture = attrService.removeAll(tenantId, entityId, scope, keys);
        addVoidCallback(deleteFuture, callback);
        addWsCallback(deleteFuture, success -> onAttributesDelete(tenantId, entityId, scope, keys, notifyDevice));
    }

    @Override
    public void deleteLatest(TenantId tenantId, @NotNull EntityId entityId, List<String> keys, @NotNull FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteLatestInternal(tenantId, entityId, keys, callback);
    }

    @Override
    public void deleteLatestInternal(TenantId tenantId, EntityId entityId, List<String> keys, @NotNull FutureCallback<Void> callback) {
        ListenableFuture<List<TsKvLatestRemovingResult>> deleteFuture = tsService.removeLatest(tenantId, entityId, keys);
        addVoidCallback(deleteFuture, callback);
    }

    @Override
    public void deleteAllLatest(TenantId tenantId, EntityId entityId, @NotNull FutureCallback<Collection<String>> callback) {
        ListenableFuture<Collection<String>> deleteFuture = tsService.removeAllLatest(tenantId, entityId);
        Futures.addCallback(deleteFuture, new FutureCallback<Collection<String>>() {
            @Override
            public void onSuccess(@Nullable Collection<String> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    @Override
    public void deleteTimeseriesAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, List<String> keys, List<DeleteTsKvQuery> deleteTsKvQueries, @NotNull FutureCallback<Void> callback) {
        ListenableFuture<List<TsKvLatestRemovingResult>> deleteFuture = tsService.remove(tenantId, entityId, deleteTsKvQueries);
        addVoidCallback(deleteFuture, callback);
        addWsCallback(deleteFuture, list -> onTimeSeriesDelete(tenantId, entityId, keys, list));
    }

    @Override
    public void saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, long value, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, String value, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, double value, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new DoubleDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, boolean value, @NotNull FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @NotNull
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, long value) {
        @NotNull SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, String value) {
        @NotNull SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, double value) {
        @NotNull SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, String key, boolean value) {
        @NotNull SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    private void onAttributesUpdate(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, @NotNull List<AttributeKvEntry> attributes, boolean notifyDevice) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                subscriptionManagerService.get().onAttributesUpdate(tenantId, entityId, scope, attributes, notifyDevice, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toAttributesUpdateProto(tenantId, entityId, scope, attributes);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private void onAttributesDelete(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, List<String> keys, boolean notifyDevice) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                subscriptionManagerService.get().onAttributesDelete(tenantId, entityId, scope, keys, notifyDevice, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toAttributesDeleteProto(tenantId, entityId, scope, keys, notifyDevice);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private void onTimeSeriesUpdate(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                subscriptionManagerService.get().onTimeSeriesUpdate(tenantId, entityId, ts, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toTimeseriesUpdateProto(tenantId, entityId, ts);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private void onTimeSeriesDelete(@NotNull TenantId tenantId, @NotNull EntityId entityId, List<String> keys, @NotNull List<TsKvLatestRemovingResult> ts) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                @NotNull List<TsKvEntry> updated = new ArrayList<>();
                @NotNull List<String> deleted = new ArrayList<>();

                ts.stream().filter(Objects::nonNull).forEach(res -> {
                    if (res.isRemoved()) {
                        if (res.getData() != null) {
                            updated.add(res.getData());
                        } else {
                            deleted.add(res.getKey());
                        }
                    }
                });

                subscriptionManagerService.get().onTimeSeriesUpdate(tenantId, entityId, updated, TbCallback.EMPTY);
                subscriptionManagerService.get().onTimeSeriesDelete(tenantId, entityId, deleted, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toTimeseriesDeleteProto(tenantId, entityId, keys);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private <S> void addVoidCallback(@NotNull ListenableFuture<S> saveFuture, @NotNull final FutureCallback<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    private <S> void addMainCallback(@NotNull ListenableFuture<S> saveFuture, @NotNull final FutureCallback<S> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    private void checkInternalEntity(@NotNull EntityId entityId) {
        if (EntityType.API_USAGE_STATE.equals(entityId.getEntityType())) {
            throw new RuntimeException("Can't update API Usage State!");
        }
    }

    private static class VoidFutureCallback implements FutureCallback<Void> {
        private final SettableFuture<Void> future;

        public VoidFutureCallback(SettableFuture<Void> future) {
            this.future = future;
        }

        @Override
        public void onSuccess(Void result) {
            future.set(null);
        }

        @Override
        public void onFailure(Throwable t) {
            future.setException(t);
        }
    }

}
