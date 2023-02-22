package org.echoiot.server.queue.memory;

import lombok.Data;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.jetbrains.annotations.Nullable;

@Data
public class InMemoryTbQueueProducer<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final InMemoryStorage storage;

    private final String defaultTopic;

    public InMemoryTbQueueProducer(InMemoryStorage storage, String defaultTopic) {
        this.storage = storage;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public void init() {

    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, @Nullable TbQueueCallback callback) {
        boolean result = storage.put(tpi.getFullTopicName(), msg);
        if (result) {
            if (callback != null) {
                callback.onSuccess(null);
            }
        } else {
            if (callback != null) {
                callback.onFailure(new RuntimeException("Failure add msg to InMemoryQueue"));
            }
        }
    }

    @Override
    public void stop() {

    }
}
