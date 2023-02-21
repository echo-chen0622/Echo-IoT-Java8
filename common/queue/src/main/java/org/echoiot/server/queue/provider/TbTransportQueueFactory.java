package org.echoiot.server.queue.provider;

import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.TbQueueRequestTemplate;
import org.echoiot.server.queue.common.TbProtoQueueMsg;

public interface TbTransportQueueFactory extends TbUsageStatsClientQueueFactory {

    TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate();

    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer();

    TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer();

}
