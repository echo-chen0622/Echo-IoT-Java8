package org.echoiot.server.service.entitiy.queue;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.scheduler.SchedulerComponent;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbQueueService extends AbstractTbEntityService implements TbQueueService {
    private static final long DELETE_DELAY = 30;

    @NotNull
    private final QueueService queueService;
    @NotNull
    private final TbClusterService tbClusterService;
    @NotNull
    private final TbQueueAdmin tbQueueAdmin;
    @NotNull
    private final SchedulerComponent scheduler;

    @NotNull
    @Override
    public Queue saveQueue(@NotNull Queue queue) {
        boolean create = queue.getId() == null;
        @Nullable Queue oldQueue;

        if (create) {
            oldQueue = null;
        } else {
            oldQueue = queueService.findQueueById(queue.getTenantId(), queue.getId());
        }

        //TODO: add checkNotNull
        Queue savedQueue = queueService.saveQueue(queue);

        if (create) {
            onQueueCreated(savedQueue);
        } else {
            onQueueUpdated(savedQueue, oldQueue);
        }

        notificationEntityService.notifySendMsgToEdgeService(queue.getTenantId(), savedQueue.getId(), create ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

        return savedQueue;
    }

    @Override
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        Queue queue = queueService.findQueueById(tenantId, queueId);
        queueService.deleteQueue(tenantId, queueId);
        onQueueDeleted(queue);
    }

    @Override
    public void deleteQueueByQueueName(TenantId tenantId, String queueName) {
        Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, queueName);
        queueService.deleteQueue(tenantId, queue.getId());
        onQueueDeleted(queue);
    }

    private void onQueueCreated(@NotNull Queue queue) {
        for (int i = 0; i < queue.getPartitions(); i++) {
            tbQueueAdmin.createTopicIfNotExists(
                    new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
        }

        tbClusterService.onQueueChange(queue);
    }

    private void onQueueUpdated(@NotNull Queue queue, @NotNull Queue oldQueue) {
        int oldPartitions = oldQueue.getPartitions();
        int currentPartitions = queue.getPartitions();

        if (currentPartitions != oldPartitions) {
            if (currentPartitions > oldPartitions) {
                log.info("Added [{}] new partitions to [{}] queue", currentPartitions - oldPartitions, queue.getName());
                for (int i = oldPartitions; i < currentPartitions; i++) {
                    tbQueueAdmin.createTopicIfNotExists(
                            new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
                tbClusterService.onQueueChange(queue);
            } else {
                log.info("Removed [{}] partitions from [{}] queue", oldPartitions - currentPartitions, queue.getName());
                tbClusterService.onQueueChange(queue);

                scheduler.schedule(() -> {
                    for (int i = currentPartitions; i < oldPartitions; i++) {
                        String fullTopicName = new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName();
                        log.info("Removed partition [{}]", fullTopicName);
                        tbQueueAdmin.deleteTopic(
                                fullTopicName);
                    }
                }, DELETE_DELAY, TimeUnit.SECONDS);
            }
        } else if (!oldQueue.equals(queue)) {
            tbClusterService.onQueueChange(queue);
        }
    }

    private void onQueueDeleted(@NotNull Queue queue) {
        tbClusterService.onQueueDelete(queue);

//        queueStatsService.deleteQueueStatsByQueueId(tenantId, queueId);

        scheduler.schedule(() -> {
            for (int i = 0; i < queue.getPartitions(); i++) {
                String fullTopicName = new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName();
                log.info("Deleting queue [{}]", fullTopicName);
                try {
                    tbQueueAdmin.deleteTopic(fullTopicName);
                } catch (Exception e) {
                    log.error("Failed to delete queue [{}]", fullTopicName);
                }
            }
        }, DELETE_DELAY, TimeUnit.SECONDS);

        notificationEntityService.notifySendMsgToEdgeService(queue.getTenantId(), queue.getId(), EdgeEventActionType.DELETED);
    }

    @Override
    public void updateQueuesByTenants(@NotNull List<TenantId> tenantIds, @NotNull TenantProfile newTenantProfile, @Nullable TenantProfile
            oldTenantProfile) {
        boolean oldIsolated = oldTenantProfile != null && oldTenantProfile.isIsolatedTbRuleEngine();
        boolean newIsolated = newTenantProfile.isIsolatedTbRuleEngine();

        if (!oldIsolated && !newIsolated) {
            return;
        }

        if (newTenantProfile.equals(oldTenantProfile)) {
            return;
        }

        Map<String, TenantProfileQueueConfiguration> oldQueues;
        Map<String, TenantProfileQueueConfiguration> newQueues;

        if (oldIsolated) {
            oldQueues = oldTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            oldQueues = Collections.emptyMap();
        }

        if (newIsolated) {
            newQueues = newTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            newQueues = Collections.emptyMap();
        }

        @NotNull List<String> toRemove = new ArrayList<>();
        @NotNull List<String> toCreate = new ArrayList<>();
        @NotNull List<String> toUpdate = new ArrayList<>();

        for (String oldQueue : oldQueues.keySet()) {
            if (!newQueues.containsKey(oldQueue)) {
                toRemove.add(oldQueue);
            }
        }

        for (String newQueue : newQueues.keySet()) {
            if (oldQueues.containsKey(newQueue)) {
                toUpdate.add(newQueue);
            } else {
                toCreate.add(newQueue);
            }
        }

        tenantIds.forEach(tenantId -> {
            toCreate.forEach(key -> saveQueue(new Queue(tenantId, newQueues.get(key))));

            toUpdate.forEach(key -> {
                @NotNull Queue queueToUpdate = new Queue(tenantId, newQueues.get(key));
                Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, key);
                queueToUpdate.setId(foundQueue.getId());
                queueToUpdate.setCreatedTime(foundQueue.getCreatedTime());

                if (!queueToUpdate.equals(foundQueue)) {
                    saveQueue(queueToUpdate);
                }
            });

            toRemove.forEach(q -> {
                Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, q);
                QueueId queueIdForRemove = queue.getId();
                deleteQueue(tenantId, queueIdForRemove);
            });
        });
    }

}
