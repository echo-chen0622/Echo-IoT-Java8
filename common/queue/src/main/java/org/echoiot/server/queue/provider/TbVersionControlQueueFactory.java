package org.echoiot.server.queue.provider;

import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for initialization of various Producers and Consumers used by TB Version Control Node.
 * Implementation Depends on the queue queue.type from yml or TB_QUEUE_TYPE environment variable
 */
public interface TbVersionControlQueueFactory extends TbUsageStatsClientQueueFactory {

    /**
     * Used to push notifications to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer();

    /**
     * Used to consume messages from TB Core Service
     *
     * @return
     */
    TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createToVersionControlMsgConsumer();

}