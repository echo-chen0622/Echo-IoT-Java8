package org.echoiot.server.service.partition;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.DonAsynchron;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
public abstract class AbstractPartitionBasedService<T extends EntityId> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final ConcurrentMap<TopicPartitionInfo, Set<T>> partitionedEntities = new ConcurrentHashMap<>();
    protected final ConcurrentMap<TopicPartitionInfo, List<ListenableFuture<?>>> partitionedFetchTasks = new ConcurrentHashMap<>();
    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    protected ListeningScheduledExecutorService scheduledExecutor;

    abstract protected String getServiceName();

    abstract protected String getSchedulerExecutorName();

    abstract protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions);

    abstract protected void cleanupEntityOnPartitionRemoval(T entityId);

    public Set<T> getPartitionedEntities(TopicPartitionInfo tpi) {
        return partitionedEntities.get(tpi);
    }

    protected void init() {
        // Should be always single threaded due to absence of locks.
        scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(EchoiotThreadFactory.forName(getSchedulerExecutorName())));
    }

    @NotNull
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    protected void stop() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    /**
     * DiscoveryService will call this event from the single thread (one-by-one).
     * Events order is guaranteed by DiscoveryService.
     * The only concurrency is expected from the [main] thread on Application started.
     * Async implementation. Locks is not allowed by design.
     * Any locks or delays in this module will affect DiscoveryService and entire system
     */
    @Override
    protected void onTbApplicationEvent(@NotNull PartitionChangeEvent partitionChangeEvent) {
        if (getServiceType().equals(partitionChangeEvent.getServiceType())) {
            log.debug("onTbApplicationEvent, processing event: {}", partitionChangeEvent);
            subscribeQueue.add(partitionChangeEvent.getPartitions());
            scheduledExecutor.submit(this::pollInitStateFromDB);
        }
    }

    protected void pollInitStateFromDB() {
        @Nullable final Set<TopicPartitionInfo> partitions = getLatestPartitions();
        if (partitions == null) {
            log.debug("Nothing to do. Partitions are empty.");
            return;
        }
        initStateFromDB(partitions);
    }

    private void initStateFromDB(@NotNull Set<TopicPartitionInfo> partitions) {
        try {
            log.info("[{}] CURRENT PARTITIONS: {}", getServiceName(), partitionedEntities.keySet());
            log.info("[{}] NEW PARTITIONS: {}", getServiceName(), partitions);

            @NotNull Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedEntities.keySet());

            log.info("[{}] ADDED PARTITIONS: {}", getServiceName(), addedPartitions);

            @NotNull Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedEntities.keySet());
            removedPartitions.removeAll(partitions);

            log.info("[{}] REMOVED PARTITIONS: {}", getServiceName(), removedPartitions);

            boolean partitionListChanged = false;
            // We no longer manage current partition of entities;
            for (var partition : removedPartitions) {
                Set<T> entities = partitionedEntities.remove(partition);
                if (entities != null) {
                    entities.forEach(this::cleanupEntityOnPartitionRemoval);
                }
                List<ListenableFuture<?>> fetchTasks = partitionedFetchTasks.remove(partition);
                if (fetchTasks != null) {
                    fetchTasks.forEach(f -> f.cancel(false));
                }
                partitionListChanged = true;
            }

            onRepartitionEvent();

            addedPartitions.forEach(tpi -> partitionedEntities.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            if (!addedPartitions.isEmpty()) {
                var fetchTasks = onAddedPartitions(addedPartitions);
                if (fetchTasks != null && !fetchTasks.isEmpty()) {
                    partitionedFetchTasks.putAll(fetchTasks);
                }
                partitionListChanged = true;
            }

            if (partitionListChanged) {
                @NotNull List<ListenableFuture<?>> partitionFetchFutures = new ArrayList<>();
                partitionedFetchTasks.values().forEach(partitionFetchFutures::addAll);
                DonAsynchron.withCallback(Futures.allAsList(partitionFetchFutures), t -> logPartitions(), this::logFailure);
            }
        } catch (Throwable t) {
            log.warn("[{}] Failed to init entities state from DB", getServiceName(), t);
        }
    }

    private void logFailure(Throwable e) {
        if (e instanceof CancellationException) {
            //Probably this is fine and happens due to re-balancing.
            log.trace("Partition fetch task error", e);
        } else {
            log.error("Partition fetch task error", e);
        }

    }

    private void logPartitions() {
        log.info("[{}] Managing following partitions:", getServiceName());
        partitionedEntities.forEach((tpi, entities) -> {
            log.info("[{}][{}]: {} entities", getServiceName(), tpi.getFullTopicName(), entities.size());
        });
    }

    protected void onRepartitionEvent() {
    }

    @Nullable
    private Set<TopicPartitionInfo> getLatestPartitions() {
        log.debug("getLatestPartitionsFromQueue, queue size {}", subscribeQueue.size());
        @Nullable Set<TopicPartitionInfo> partitions = null;
        while (!subscribeQueue.isEmpty()) {
            partitions = subscribeQueue.poll();
            log.debug("polled from the queue partitions {}", partitions);
        }
        log.debug("getLatestPartitionsFromQueue, partitions {}", partitions);
        return partitions;
    }

}
