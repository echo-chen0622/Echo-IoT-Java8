package org.echoiot.server.queue.rabbitmq;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TbRabbitMqProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbRabbitMqSettings rabbitMqSettings;
    @NotNull
    private final ListeningExecutorService producerExecutor;
    private final Channel channel;
    private final Connection connection;

    private final Set<TopicPartitionInfo> topics = ConcurrentHashMap.newKeySet();

    public TbRabbitMqProducerTemplate(TbQueueAdmin admin, @NotNull TbRabbitMqSettings rabbitMqSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.rabbitMqSettings = rabbitMqSettings;
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
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
        createTopicIfNotExist(tpi);
        @NotNull AMQP.BasicProperties properties = new AMQP.BasicProperties();
        try {
            channel.basicPublish(rabbitMqSettings.getExchangeName(), tpi.getFullTopicName(), properties, gson.toJson(new DefaultTbQueueMsg(msg)).getBytes());
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (IOException e) {
            log.error("Failed publish message: [{}].", msg, e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    @Override
    public void stop() {
        if (producerExecutor != null) {
            producerExecutor.shutdownNow();
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close the channel.");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close the connection.");
            }
        }
    }

    private void createTopicIfNotExist(@NotNull TopicPartitionInfo tpi) {
        if (topics.contains(tpi)) {
            return;
        }
        admin.createTopicIfNotExists(tpi.getFullTopicName());
        topics.add(tpi);
    }
}
