package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;

/**
 * Created by ashvayka on 05.10.18.
 */
public class ToTransportMsgResponseDecoder implements TbKafkaDecoder<ToTransportMsg> {

    @Override
    public ToTransportMsg decode(TbQueueMsg msg) throws IOException {
        return ToTransportMsg.parseFrom(msg.getData());
    }
}
