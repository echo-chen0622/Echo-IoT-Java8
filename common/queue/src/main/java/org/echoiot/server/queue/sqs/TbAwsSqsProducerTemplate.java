package org.echoiot.server.queue.sqs;

import com.amazonaws.auth.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.util.concurrent.*;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Slf4j
public class TbAwsSqsProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final AmazonSQS sqsClient;
    private final Gson gson = new Gson();
    private final Map<String, String> queueUrlMap = new ConcurrentHashMap<>();
    private final TbQueueAdmin admin;
    private final ListeningExecutorService producerExecutor;

    public TbAwsSqsProducerTemplate(TbQueueAdmin admin, @NotNull TbAwsSqsSettings sqsSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;

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
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(@NotNull TopicPartitionInfo tpi, @NotNull T msg, @Nullable TbQueueCallback callback) {
        @NotNull SendMessageRequest sendMsgRequest = new SendMessageRequest();
        sendMsgRequest.withQueueUrl(getQueueUrl(tpi.getFullTopicName()));
        sendMsgRequest.withMessageBody(gson.toJson(new DefaultTbQueueMsg(msg)));

        String sqsMsgId = UUID.randomUUID().toString();
        sendMsgRequest.withMessageGroupId(sqsMsgId);
        sendMsgRequest.withMessageDeduplicationId(sqsMsgId);

        @NotNull ListenableFuture<SendMessageResult> future = producerExecutor.submit(() -> sqsClient.sendMessage(sendMsgRequest));

        Futures.addCallback(future, new FutureCallback<SendMessageResult>() {
            @Override
            public void onSuccess(@NotNull SendMessageResult result) {
                if (callback != null) {
                    callback.onSuccess(new AwsSqsTbQueueMsgMetadata(result.getSdkHttpMetadata()));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        }, producerExecutor);
    }

    @Override
    public void stop() {
        if (producerExecutor != null) {
            producerExecutor.shutdownNow();
        }
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    private String getQueueUrl(@NotNull String topic) {
        return queueUrlMap.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
        });
    }
}
