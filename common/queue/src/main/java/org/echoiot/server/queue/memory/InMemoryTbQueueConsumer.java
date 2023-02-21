package org.echoiot.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final InMemoryStorage storage;
    private volatile Set<TopicPartitionInfo> partitions;
    private volatile boolean stopped;
    private volatile boolean subscribed;

    public InMemoryTbQueueConsumer(InMemoryStorage storage, String topic) {
        this.storage = storage;
        this.topic = topic;
        stopped = false;
    }

    private final String topic;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
        subscribed = true;
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = true;
    }

    @Override
    public void unsubscribe() {
        stopped = true;
    }

    @NotNull
    @Override
    public List<T> poll(long durationInMillis) {
        if (subscribed) {
            @NotNull @SuppressWarnings("unchecked")
            List<T> messages = partitions
                    .stream()
                    .map(tpi -> {
                        try {
                            return storage.get(tpi.getFullTopicName());
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                log.error("Queue was interrupted.", e);
                            }
                            return Collections.emptyList();
                        }
                    })
                    .flatMap(List::stream)
                    .map(msg -> (T) msg).collect(Collectors.toList());
            if (messages.size() > 0) {
                return messages;
            }
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.error("Failed to sleep.", e);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

}
