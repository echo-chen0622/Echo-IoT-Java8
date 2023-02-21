package org.echoiot.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.echoiot.server.service.subscription.SubscriptionManagerService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by Echo on 27.03.18.
 */
@Slf4j
public abstract class AbstractSubscriptionService extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    protected final TbClusterService clusterService;
    protected final PartitionService partitionService;
    protected Optional<SubscriptionManagerService> subscriptionManagerService;

    protected ExecutorService wsCallBackExecutor;

    public AbstractSubscriptionService(TbClusterService clusterService,
                                       PartitionService partitionService) {
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Autowired(required = false)
    public void setSubscriptionManagerService(Optional<SubscriptionManagerService> subscriptionManagerService) {
        this.subscriptionManagerService = subscriptionManagerService;
    }

    abstract String getExecutorPrefix();

    @PostConstruct
    public void initExecutor() {
        wsCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName(getExecutorPrefix() + "-service-ws-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(@NotNull PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
        }
    }

    protected <T> void addWsCallback(@NotNull ListenableFuture<T> saveFuture, @NotNull Consumer<T> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, wsCallBackExecutor);
    }
}
