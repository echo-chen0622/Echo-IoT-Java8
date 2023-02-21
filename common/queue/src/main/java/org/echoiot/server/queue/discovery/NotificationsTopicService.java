package org.echoiot.server.queue.discovery;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationsTopicService {

    private final Map<String, TopicPartitionInfo> tbCoreNotificationTopics = new HashMap<>();
    private final Map<String, TopicPartitionInfo> tbRuleEngineNotificationTopics = new HashMap<>();

    /**
     * Each Service should start a consumer for messages that target individual service instance based on serviceId.
     * This topic is likely to have single partition, and is always assigned to the service.
     * @param serviceType
     * @param serviceId
     * @return
     */
    @NotNull
    public TopicPartitionInfo getNotificationsTopic(@NotNull ServiceType serviceType, String serviceId) {
        switch (serviceType) {
            case TB_CORE:
                return tbCoreNotificationTopics.computeIfAbsent(serviceId,
                        id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            case TB_RULE_ENGINE:
                return tbRuleEngineNotificationTopics.computeIfAbsent(serviceId,
                        id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            default:
                return buildNotificationsTopicPartitionInfo(serviceType, serviceId);
        }
    }

    @NotNull
    private TopicPartitionInfo buildNotificationsTopicPartitionInfo(@NotNull ServiceType serviceType, String serviceId) {
        return new TopicPartitionInfo(serviceType.name().toLowerCase() + ".notifications." + serviceId, null, null, false);
    }
}
