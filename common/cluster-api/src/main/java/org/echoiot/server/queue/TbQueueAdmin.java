package org.echoiot.server.queue;

public interface TbQueueAdmin {

    void createTopicIfNotExists(String topic);

    void destroy();

    void deleteTopic(String topic);
}
