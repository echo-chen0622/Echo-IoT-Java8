package org.echoiot.server.queue.azure.servicebus;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.microsoft.azure.servicebus.TransactionContext;
import com.microsoft.azure.servicebus.primitives.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueMsgDecoder;
import org.echoiot.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.echoiot.server.queue.common.DefaultTbQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TbServiceBusConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<MessageWithDeliveryTag, T> {
    private final TbQueueAdmin admin;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbServiceBusSettings serviceBusSettings;

    private final Gson gson = new Gson();

    private Set<CoreMessageReceiver> receivers;
    private final Map<CoreMessageReceiver, Collection<MessageWithDeliveryTag>> pendingMessages = new ConcurrentHashMap<>();
    private volatile int messagesPerQueue;

    public TbServiceBusConsumerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.serviceBusSettings = serviceBusSettings;
    }

    @Override
    protected List<MessageWithDeliveryTag> doPoll(long durationInMillis) {
        @NotNull List<CompletableFuture<Collection<MessageWithDeliveryTag>>> messageFutures =
                receivers.stream()
                        .map(receiver -> receiver
                                .receiveAsync(messagesPerQueue, Duration.ofMillis(durationInMillis))
                                .whenComplete((messages, err) -> {
                                    if (!CollectionUtils.isEmpty(messages)) {
                                        pendingMessages.put(receiver, messages);
                                    } else if (err != null) {
                                        log.error("Failed to receive messages.", err);
                                    }
                                }))
                        .collect(Collectors.toList());
        try {
            return fromList(messageFutures)
                    .get()
                    .stream()
                    .flatMap(messages -> CollectionUtils.isEmpty(messages) ? Stream.empty() : messages.stream())
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to receive messages", e);
            }
            return Collections.emptyList();
        }
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        createReceivers();
        messagesPerQueue = receivers.size() / Math.max(partitions.size(), 1);
    }

    @Override
    protected void doCommit() {
        pendingMessages.forEach((receiver, msgs) ->
                msgs.forEach(msg -> receiver.completeMessageAsync(msg.getDeliveryTag(), TransactionContext.NULL_TXN)));
        pendingMessages.clear();
    }

    @Override
    protected void doUnsubscribe() {
        receivers.forEach(CoreMessageReceiver::closeAsync);
    }

    private void createReceivers() {
        @NotNull List<CompletableFuture<CoreMessageReceiver>> receiverFutures = partitions.stream()
                                                                                          .map(TopicPartitionInfo::getFullTopicName)
                                                                                          .map(queue -> {
                    MessagingFactory factory;
                    try {
                        factory = MessagingFactory.createFromConnectionStringBuilder(createConnection(queue));
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to create factory for the queue [{}]", queue);
                        throw new RuntimeException("Failed to create the factory", e);
                    }

                    return CoreMessageReceiver.create(factory, queue, queue, 0,
                            new SettleModePair(SenderSettleMode.UNSETTLED, ReceiverSettleMode.SECOND),
                            MessagingEntityType.QUEUE);
                }).collect(Collectors.toList());

        try {
            receivers = new HashSet<>(fromList(receiverFutures).get());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to create receivers", e);
            }
        }
    }

    @NotNull
    private ConnectionStringBuilder createConnection(String queue) {
        admin.createTopicIfNotExists(queue);
        return new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                queue,
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());
    }

    private <V> CompletableFuture<List<V>> fromList(@NotNull List<CompletableFuture<V>> futures) {
        @NotNull @SuppressWarnings("unchecked")
        CompletableFuture<Collection<V>>[] arrayFuture = new CompletableFuture[futures.size()];
        futures.toArray(arrayFuture);

        return CompletableFuture
                .allOf(arrayFuture)
                .thenApply(v -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    @Override
    protected T decode(@NotNull MessageWithDeliveryTag data) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(((Data) data.getMessage().getBody()).getValue().getArray()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}
