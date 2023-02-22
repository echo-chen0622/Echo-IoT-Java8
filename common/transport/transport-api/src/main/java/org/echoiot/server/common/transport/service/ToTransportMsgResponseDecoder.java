package org.echoiot.server.common.transport.service;

import org.echoiot.server.gen.transport.TransportProtos.ToTransportMsg;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;

/**
 * Created by Echo on 05.10.18.
 */
public class ToTransportMsgResponseDecoder implements TbKafkaDecoder<ToTransportMsg> {

    @Override
    public ToTransportMsg decode(TbQueueMsg msg) throws IOException {
        return ToTransportMsg.parseFrom(msg.getData());
    }
}
