package org.echoiot.server.queue.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueMsgDecoder;
import org.echoiot.server.queue.common.AbstractParallelTbQueueConsumerTemplate;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TbAwsSqsConsumerTemplate<T extends TbQueueMsg> extends AbstractParallelTbQueueConsumerTemplate<Message, T> {

    private static final int MAX_NUM_MSGS = 10;

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final AmazonSQS sqsClient;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbAwsSqsSettings sqsSettings;

    private final List<AwsSqsMsgWrapper> pendingMessages = new CopyOnWriteArrayList<>();
    private volatile Set<String> queueUrls;

    public TbAwsSqsConsumerTemplate(TbQueueAdmin admin, @NotNull TbAwsSqsSettings sqsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.sqsSettings = sqsSettings;

        AWSCredentialsProvider credentialsProvider;
        if (sqsSettings.getUseDefaultCredentialProviderChain()) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            @NotNull AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        }

        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

    }

    @Override
    protected void doSubscribe(@NotNull List<String> topicNames) {
        queueUrls = topicNames.stream().map(this::getQueueUrl).collect(Collectors.toSet());
        initNewExecutor(queueUrls.size() * sqsSettings.getThreadsPerTopic() + 1);
    }

    @NotNull
    @Override
    protected List<Message> doPoll(long durationInMillis) {
        int duration = (int) TimeUnit.MILLISECONDS.toSeconds(durationInMillis);
        @NotNull List<ListenableFuture<List<Message>>> futureList = queueUrls
                .stream()
                .map(url -> poll(url, duration))
                .collect(Collectors.toList());
        @NotNull ListenableFuture<List<List<Message>>> futureResult = Futures.allAsList(futureList);
        try {
            return futureResult.get().stream()
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Aws SQS consumer is stopped.", getTopic());
            } else {
                log.error("Failed to pool messages.", e);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public T decode(@NotNull Message message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getBody(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

    @Override
    protected void doCommit() {
        pendingMessages.forEach(msg ->
                consumerExecutor.submit(() -> {
                    @NotNull List<DeleteMessageBatchRequestEntry> entries = msg.getMessages()
                                                                               .stream()
                                                                               .map(message -> new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()))
                                                                               .collect(Collectors.toList());
                    sqsClient.deleteMessageBatch(msg.getUrl(), entries);
                }));
        pendingMessages.clear();
    }

    @Override
    protected void doUnsubscribe() {
        stopped = true;
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        shutdownExecutor();
    }

    @NotNull
    private ListenableFuture<List<Message>> poll(String url, int waitTimeSeconds) {
        @NotNull List<ListenableFuture<List<Message>>> result = new ArrayList<>();

        for (int i = 0; i < sqsSettings.getThreadsPerTopic(); i++) {
            result.add(consumerExecutor.submit(() -> {
                @NotNull ReceiveMessageRequest request = new ReceiveMessageRequest();
                request
                        .withWaitTimeSeconds(waitTimeSeconds)
                        .withQueueUrl(url)
                        .withMaxNumberOfMessages(MAX_NUM_MSGS);
                return sqsClient.receiveMessage(request).getMessages();
            }));
        }
        return Futures.transform(Futures.allAsList(result), list -> {
            if (!CollectionUtils.isEmpty(list)) {
                return list.stream()
                        .flatMap(messageList -> {
                            if (!messageList.isEmpty()) {
                                this.pendingMessages.add(new AwsSqsMsgWrapper(url, messageList));
                                return messageList.stream();
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);
    }

    @Data
    private static class AwsSqsMsgWrapper {
        private final String url;
        private final List<Message> messages;

        public AwsSqsMsgWrapper(String url, List<Message> messages) {
            this.url = url;
            this.messages = messages;
        }
    }

    private String getQueueUrl(@NotNull String topic) {
        admin.createTopicIfNotExists(topic);
        return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
    }
}
