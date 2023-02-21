package org.echoiot.server.queue.sqs;

import com.amazonaws.auth.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueAdmin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final Map<String, String> attributes;
    private final AmazonSQS sqsClient;
    @NotNull
    private final Map<String, String> queues;

    public TbAwsSqsAdmin(@NotNull TbAwsSqsSettings sqsSettings, Map<String, String> attributes) {
        this.attributes = attributes;

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

        queues = sqsClient
                .listQueues()
                .getQueueUrls()
                .stream()
                .map(this::getQueueNameFromUrl)
                .collect(Collectors.toMap(this::convertTopicToQueueName, Function.identity()));
    }

    @Override
    public void createTopicIfNotExists(@NotNull String topic) {
        @NotNull String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            return;
        }
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);
        String queueUrl = sqsClient.createQueue(createQueueRequest).getQueueUrl();
        queues.put(getQueueNameFromUrl(queueUrl), queueUrl);
    }

    @NotNull
    private String convertTopicToQueueName(@NotNull String topic) {
        return topic.replaceAll("\\.", "_") + ".fifo";
    }

    @Override
    public void deleteTopic(@NotNull String topic) {
        @NotNull String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            sqsClient.deleteQueue(queues.get(queueName));
        } else {
            GetQueueUrlResult queueUrl = sqsClient.getQueueUrl(queueName);
            if (queueUrl != null) {
                sqsClient.deleteQueue(queueUrl.getQueueUrl());
            } else {
                log.warn("Aws SQS queue [{}] does not exist!", queueName);
            }
        }
    }

    @NotNull
    private String getQueueNameFromUrl(@NotNull String queueUrl) {
        int delimiterIndex = queueUrl.lastIndexOf("/");
        return queueUrl.substring(delimiterIndex + 1);
    }

    @Override
    public void destroy() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }
}
