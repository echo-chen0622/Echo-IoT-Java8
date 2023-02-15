package org.thingsboard.server.service.transport;

import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;

/**
 * Created by ashvayka on 05.10.18.
 */
public interface TransportApiService extends TbQueueHandler<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> {
}
