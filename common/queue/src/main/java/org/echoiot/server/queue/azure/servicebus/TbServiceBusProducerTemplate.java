package org.echoiot.server.queue.azure.servicebus;

import com.google.gson.Gson;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TbServiceBusProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbServiceBusSettings serviceBusSettings;
    private final Map<String, QueueClient> clients = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public TbServiceBusProducerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.serviceBusSettings = serviceBusSettings;
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        IMessage message = new Message(gson.toJson(new DefaultTbQueueMsg(msg)));
        CompletableFuture<Void> future = getClient(tpi.getFullTopicName()).sendAsync(message);
        future.whenCompleteAsync((success, err) -> {
            if (err != null) {
                callback.onFailure(err);
            } else {
                callback.onSuccess(null);
            }
        }, executorService);
    }

    @Override
    public void stop() {
        clients.forEach((t, client) -> {
            try {
                client.close();
            } catch (ServiceBusException e) {
                log.error("Failed to close QueueClient.", e);
            }
        });

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private QueueClient getClient(String topic) {
        return clients.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            ConnectionStringBuilder builder =
                    new ConnectionStringBuilder(
                            serviceBusSettings.getNamespaceName(),
                            topic,
                            serviceBusSettings.getSasKeyName(),
                            serviceBusSettings.getSasKey());
            try {
                return new QueueClient(builder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException | ServiceBusException e) {
                log.error("Failed to create new client for the Queue: [{}]", topic, e);
                throw new RuntimeException("Failed to create new client for the Queue", e);
            }
        });
    }
}
