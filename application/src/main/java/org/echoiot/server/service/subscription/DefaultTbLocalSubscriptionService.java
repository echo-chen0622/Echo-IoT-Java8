package org.echoiot.server.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotExecutors;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbLocalSubscriptionService implements TbLocalSubscriptionService {

    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<Integer, TbSubscription>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Resource
    private PartitionService partitionService;

    @Resource
    private TbClusterService clusterService;

    @Resource
    @Lazy
    private SubscriptionManagerService subscriptionManagerService;

    private ExecutorService subscriptionUpdateExecutor;

    private final TbApplicationEventListener<PartitionChangeEvent> partitionChangeListener = new TbApplicationEventListener<>() {
        @Override
        protected void onTbApplicationEvent(@NotNull PartitionChangeEvent event) {
            if (ServiceType.TB_CORE.equals(event.getServiceType())) {
                currentPartitions.clear();
                currentPartitions.addAll(event.getPartitions());
            }
        }
    };

    private final TbApplicationEventListener<ClusterTopologyChangeEvent> clusterTopologyChangeListener = new TbApplicationEventListener<>() {
        @Override
        protected void onTbApplicationEvent(@NotNull ClusterTopologyChangeEvent event) {
            if (event.getQueueKeys().stream().anyMatch(key -> ServiceType.TB_CORE.equals(key.getType()))) {
                /*
                 * If the cluster topology has changed, we need to push all current subscriptions to SubscriptionManagerService again.
                 * Otherwise, the SubscriptionManagerService may "forget" those subscriptions in case of restart.
                 * Although this is resource consuming operation, it is cheaper than sending ping/pong commands periodically
                 * It is also cheaper then caching the subscriptions by entity id and then lookup of those caches every time we have new telemetry in SubscriptionManagerService.
                 * Even if we cache locally the list of active subscriptions by entity id, it is still time consuming operation to get them from cache
                 * Since number of subscriptions is usually much less then number of devices that are pushing data.
                 */
                subscriptionsBySessionId.values().forEach(map -> map.values()
                        .forEach(sub -> pushSubscriptionToManagerService(sub, true)));
            }
        }
    };

    @PostConstruct
    public void initExecutor() {
        subscriptionUpdateExecutor = EchoiotExecutors.newWorkStealingPool(20, getClass());
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (subscriptionUpdateExecutor != null) {
            subscriptionUpdateExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(PartitionChangeEvent.class)
    public void onApplicationEvent(@NotNull PartitionChangeEvent event) {
        partitionChangeListener.onApplicationEvent(event);
    }

    @Override
    @EventListener(ClusterTopologyChangeEvent.class)
    public void onApplicationEvent(@NotNull ClusterTopologyChangeEvent event) {
        clusterTopologyChangeListener.onApplicationEvent(event);
    }

    //TODO 3.1: replace null callbacks with callbacks from websocket service.
    @Override
    public void addSubscription(@NotNull TbSubscription subscription) {
        pushSubscriptionToManagerService(subscription, true);
        registerSubscription(subscription);
    }

    private void pushSubscriptionToManagerService(@NotNull TbSubscription subscription, boolean pushToLocalService) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, subscription.getTenantId(), subscription.getEntityId());
        if (currentPartitions.contains(tpi)) {
            // Subscription is managed on the same server;
            if (pushToLocalService) {
                subscriptionManagerService.addSubscription(subscription, TbCallback.EMPTY);
            }
        } else {
            // Push to the queue;
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toNewSubscriptionProto(subscription);
            clusterService.pushMsgToCore(tpi, subscription.getEntityId().getId(), toCoreMsg, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSubscriptionUpdate(String sessionId, @NotNull TelemetrySubscriptionUpdate update, @NotNull TbCallback callback) {
        TbSubscription subscription = subscriptionsBySessionId
                .getOrDefault(sessionId, Collections.emptyMap()).get(update.getSubscriptionId());
        if (subscription != null) {
            switch (subscription.getType()) {
                case TIMESERIES:
                    @NotNull TbTimeseriesSubscription tsSub = (TbTimeseriesSubscription) subscription;
                    update.getLatestValues().forEach((key, value) -> tsSub.getKeyStates().put(key, value));
                    break;
                case ATTRIBUTES:
                    @NotNull TbAttributeSubscription attrSub = (TbAttributeSubscription) subscription;
                    update.getLatestValues().forEach((key, value) -> attrSub.getKeyStates().put(key, value));
                    break;
            }
            subscriptionUpdateExecutor.submit(() -> subscription.getUpdateConsumer().accept(sessionId, update));
        }
        callback.onSuccess();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSubscriptionUpdate(String sessionId, @NotNull AlarmSubscriptionUpdate update, @NotNull TbCallback callback) {
        TbSubscription subscription = subscriptionsBySessionId
                .getOrDefault(sessionId, Collections.emptyMap()).get(update.getSubscriptionId());
        if (subscription != null && subscription.getType() == TbSubscriptionType.ALARMS) {
            subscriptionUpdateExecutor.submit(() -> subscription.getUpdateConsumer().accept(sessionId, update));
        }
        callback.onSuccess();
    }

    @Override
    public void cancelSubscription(String sessionId, int subscriptionId) {
        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
        Map<Integer, TbSubscription> sessionSubscriptions = subscriptionsBySessionId.get(sessionId);
        if (sessionSubscriptions != null) {
            TbSubscription subscription = sessionSubscriptions.remove(subscriptionId);
            if (subscription != null) {
                if (sessionSubscriptions.isEmpty()) {
                    subscriptionsBySessionId.remove(sessionId);
                }
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, subscription.getTenantId(), subscription.getEntityId());
                if (currentPartitions.contains(tpi)) {
                    // Subscription is managed on the same server;
                    subscriptionManagerService.cancelSubscription(sessionId, subscriptionId, TbCallback.EMPTY);
                } else {
                    // Push to the queue;
                    TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toCloseSubscriptionProto(subscription);
                    clusterService.pushMsgToCore(tpi, subscription.getEntityId().getId(), toCoreMsg, null);
                }
            } else {
                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
        Map<Integer, TbSubscription> subscriptions = subscriptionsBySessionId.get(sessionId);
        if (subscriptions != null) {
            @NotNull Set<Integer> toRemove = new HashSet<>(subscriptions.keySet());
            toRemove.forEach(id -> cancelSubscription(sessionId, id));
        }
    }

    private void registerSubscription(@NotNull TbSubscription subscription) {
        @NotNull Map<Integer, TbSubscription> sessionSubscriptions = subscriptionsBySessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
    }

}
