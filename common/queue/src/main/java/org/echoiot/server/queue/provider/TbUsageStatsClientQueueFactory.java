package org.echoiot.server.queue.provider;

import org.echoiot.server.queue.TbQueueProducer;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;

public interface TbUsageStatsClientQueueFactory {

    TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer();

}
