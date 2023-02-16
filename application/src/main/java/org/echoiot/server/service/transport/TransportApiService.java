package org.echoiot.server.service.transport;

import org.echoiot.server.queue.TbQueueHandler;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportApiResponseMsg;

/**
 * Created by Echo on 05.10.18.
 */
public interface TransportApiService extends TbQueueHandler<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> {
}
