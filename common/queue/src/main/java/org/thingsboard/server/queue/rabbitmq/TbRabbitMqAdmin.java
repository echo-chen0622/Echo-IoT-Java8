package org.thingsboard.server.queue.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TbRabbitMqAdmin implements TbQueueAdmin {

    private final Channel channel;
    private final Connection connection;
    private final Map<String, Object> arguments;

    public TbRabbitMqAdmin(TbRabbitMqSettings rabbitMqSettings, Map<String, Object> arguments) {
        this.arguments = arguments;

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
    public void createTopicIfNotExists(String topic) {
        try {
            channel.queueDeclare(topic, false, false, false, arguments);
        } catch (IOException e) {
            log.error("Failed to bind queue: [{}]", topic, e);
        }
    }

    @Override
    public void deleteTopic(String topic) {
        try {
            channel.queueDelete(topic);
        } catch (IOException e) {
            log.error("Failed to delete RabbitMq queue [{}].", topic);
        }
    }

    @Override
    public void destroy() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close Chanel.", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close Connection.", e);
            }
        }
    }
}
