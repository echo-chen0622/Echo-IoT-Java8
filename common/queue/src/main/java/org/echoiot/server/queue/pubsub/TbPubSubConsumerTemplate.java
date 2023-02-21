package org.echoiot.server.queue.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueMsgDecoder;
import org.echoiot.server.queue.common.AbstractParallelTbQueueConsumerTemplate;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class TbPubSubConsumerTemplate<T extends TbQueueMsg> extends AbstractParallelTbQueueConsumerTemplate<PubsubMessage, T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbPubSubSettings pubSubSettings;

    private volatile Set<String> subscriptionNames;
    private final List<AcknowledgeRequest> acknowledgeRequests = new CopyOnWriteArrayList<>();

    @NotNull
    private final SubscriberStub subscriber;
    private volatile int messagesPerTopic;

    public TbPubSubConsumerTemplate(TbQueueAdmin admin, @NotNull TbPubSubSettings pubSubSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
        this.topic = topic;
        this.decoder = decoder;
        try {
            SubscriberStubSettings subscriberStubSettings =
                    SubscriberStubSettings.newBuilder()
                            .setCredentialsProvider(pubSubSettings.getCredentialsProvider())
                            .setTransportChannelProvider(
                                    SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
                                            .setMaxInboundMessageSize(pubSubSettings.getMaxMsgSize())
                                            .build())
                            .build();
            this.subscriber = GrpcSubscriberStub.create(subscriberStubSettings);
        } catch (IOException e) {
            log.error("Failed to create subscriber.", e);
            throw new RuntimeException("Failed to create subscriber.", e);
        }
    }

    @NotNull
    @Override
    protected List<PubsubMessage> doPoll(long durationInMillis) {
        try {
            List<ReceivedMessage> messages = receiveMessages();
            if (!messages.isEmpty()) {
                return messages.stream().map(ReceivedMessage::getMessage).collect(Collectors.toList());
            }
        } catch (ExecutionException | InterruptedException e) {
            if (stopped) {
                log.info("[{}] Pub/Sub consumer is stopped.", topic);
            } else {
                log.error("Failed to receive messages", e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected void doSubscribe(@NotNull List<String> topicNames) {
        subscriptionNames = new LinkedHashSet<>(topicNames);
        subscriptionNames.forEach(admin::createTopicIfNotExists);
        initNewExecutor(subscriptionNames.size() + 1);
        messagesPerTopic = pubSubSettings.getMaxMessages() / Math.max(subscriptionNames.size(), 1);
    }

    @Override
    protected void doCommit() {
        acknowledgeRequests.forEach(subscriber.acknowledgeCallable()::futureCall);
        acknowledgeRequests.clear();
    }

    @Override
    protected void doUnsubscribe() {
        if (subscriber != null) {
            subscriber.close();
        }
        shutdownExecutor();
    }

    private List<ReceivedMessage> receiveMessages() throws ExecutionException, InterruptedException {
        @NotNull List<ApiFuture<List<ReceivedMessage>>> result = subscriptionNames.stream().map(subscriptionId -> {
            String subscriptionName = ProjectSubscriptionName.format(pubSubSettings.getProjectId(), subscriptionId);
            @NotNull PullRequest pullRequest =
                    PullRequest.newBuilder()
                            .setMaxMessages(messagesPerTopic)
//                            .setReturnImmediately(false) // return immediately if messages are not available
                            .setSubscription(subscriptionName)
                            .build();

            ApiFuture<PullResponse> pullResponseApiFuture = subscriber.pullCallable().futureCall(pullRequest);

            return ApiFutures.transform(pullResponseApiFuture, pullResponse -> {
                if (pullResponse != null && !pullResponse.getReceivedMessagesList().isEmpty()) {
                    @NotNull List<String> ackIds = new ArrayList<>();
                    for (@NotNull ReceivedMessage message : pullResponse.getReceivedMessagesList()) {
                        ackIds.add(message.getAckId());
                    }
                    @NotNull AcknowledgeRequest acknowledgeRequest =
                            AcknowledgeRequest.newBuilder()
                                    .setSubscription(subscriptionName)
                                    .addAllAckIds(ackIds)
                                    .build();

                    acknowledgeRequests.add(acknowledgeRequest);
                    return pullResponse.getReceivedMessagesList();
                }
                return null;
            }, consumerExecutor);

        }).collect(Collectors.toList());

        @NotNull ApiFuture<List<ReceivedMessage>> transform = ApiFutures.transform(ApiFutures.allAsList(result), listMessages -> {
            if (!CollectionUtils.isEmpty(listMessages)) {
                return listMessages.stream().filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);

        return transform.get();
    }

    @Override
    public T decode(@NotNull PubsubMessage message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getData().toStringUtf8(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}
