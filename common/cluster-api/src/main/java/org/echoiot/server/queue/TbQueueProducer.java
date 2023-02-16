package org.echoiot.server.queue;

import org.echoiot.server.common.msg.queue.TopicPartitionInfo;

public interface TbQueueProducer<T extends TbQueueMsg> {

    void init();

    String getDefaultTopic();

    void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback);

    void stop();
}
