package org.echoiot.server.queue.pubsub;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueAdmin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {
    private static final String ACK_DEADLINE = "ackDeadlineInSec";
    private static final String MESSAGE_RETENTION = "messageRetentionInSec";

    @NotNull
    private final TopicAdminClient topicAdminClient;
    @NotNull
    private final SubscriptionAdminClient subscriptionAdminClient;

    private final TbPubSubSettings pubSubSettings;
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();
    private final Map<String, String> subscriptionProperties;

    public TbPubSubAdmin(@NotNull TbPubSubSettings pubSubSettings, Map<String, String> subscriptionSettings) {
        this.pubSubSettings = pubSubSettings;
        this.subscriptionProperties = subscriptionSettings;

        TopicAdminSettings topicAdminSettings;
        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        SubscriptionAdminSettings subscriptionAdminSettings;
        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }

        try {
            topicAdminClient = TopicAdminClient.create(topicAdminSettings);

            @NotNull ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (@NotNull Topic topic : response.iterateAll()) {
                topicSet.add(topic.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get topics.", e);
            throw new RuntimeException("Failed to get topics.", e);
        }

        try {
            subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);

            @NotNull ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (@NotNull Subscription subscription : response.iterateAll()) {
                subscriptionSet.add(subscription.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get subscriptions.", e);
            throw new RuntimeException("Failed to get subscriptions.", e);
        }
    }

    @Override
    public void createTopicIfNotExists(String partition) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(partition)
                .setProject(pubSubSettings.getProjectId())
                .build();

        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

        @NotNull ListTopicsRequest listTopicsRequest =
                ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
        TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
        for (@NotNull Topic topic : response.iterateAll()) {
            if (topic.getName().contains(topicName.toString())) {
                topicSet.add(topic.getName());
                createSubscriptionIfNotExists(partition, topicName);
                return;
            }
        }

        try {
            topicAdminClient.createTopic(topicName);
            log.info("Created new topic: [{}]", topicName);
        } catch (AlreadyExistsException e) {
            log.info("[{}] Topic already exist.", topicName);
        } finally {
            topicSet.add(topicName.toString());
        }
        createSubscriptionIfNotExists(partition, topicName);
    }

    @Override
    public void deleteTopic(String topic) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(topic)
                .setProject(pubSubSettings.getProjectId())
                .build();

        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), topic);

        if (topicSet.contains(topicName.toString())) {
            topicAdminClient.deleteTopic(topicName);
        } else {
            if (topicAdminClient.getTopic(topicName) != null) {
                topicAdminClient.deleteTopic(topicName);
            } else {
                log.warn("PubSub topic [{}] does not exist.", topic);
            }
        }

        if (subscriptionSet.contains(subscriptionName.toString())) {
            subscriptionAdminClient.deleteSubscription(subscriptionName);
        } else {
            if (subscriptionAdminClient.getSubscription(subscriptionName) != null) {
                subscriptionAdminClient.deleteSubscription(subscriptionName);
            } else {
                log.warn("PubSub subscription [{}] does not exist.", topic);
            }
        }
    }

    private void createSubscriptionIfNotExists(String partition, @NotNull TopicName topicName) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), partition);

        if (subscriptionSet.contains(subscriptionName.toString())) {
            return;
        }

        @NotNull ListSubscriptionsRequest listSubscriptionsRequest =
                ListSubscriptionsRequest.newBuilder().setProject(ProjectName.of(pubSubSettings.getProjectId()).toString()).build();
        SubscriptionAdminClient.ListSubscriptionsPagedResponse response = subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);
        for (@NotNull Subscription subscription : response.iterateAll()) {
            if (subscription.getName().equals(subscriptionName.toString())) {
                subscriptionSet.add(subscription.getName());
                return;
            }
        }

        @NotNull Subscription.Builder subscriptionBuilder = Subscription
                .newBuilder()
                .setName(subscriptionName.toString())
                .setTopic(topicName.toString());

        setAckDeadline(subscriptionBuilder);
        setMessageRetention(subscriptionBuilder);

        try {
            subscriptionAdminClient.createSubscription(subscriptionBuilder.build());
            log.info("Created new subscription: [{}]", subscriptionName);
        } catch (AlreadyExistsException e) {
            log.info("[{}] Subscription already exist.", subscriptionName);
        } finally {
            subscriptionSet.add(subscriptionName.toString());
        }
    }

    private void setAckDeadline(@NotNull Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(ACK_DEADLINE)) {
            builder.setAckDeadlineSeconds(Integer.parseInt(subscriptionProperties.get(ACK_DEADLINE)));
        }
    }

    private void setMessageRetention(@NotNull Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(MESSAGE_RETENTION)) {
            @NotNull Duration duration = Duration
                    .newBuilder()
                    .setSeconds(Long.parseLong(subscriptionProperties.get(MESSAGE_RETENTION)))
                    .build();
            builder.setMessageRetentionDuration(duration);
        }
    }

    @Override
    public void destroy() {
        if (topicAdminClient != null) {
            topicAdminClient.close();
        }
        if (subscriptionAdminClient != null) {
            subscriptionAdminClient.close();
        }
    }
}
