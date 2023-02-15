package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;

/**
 * Created by ashvayka on 05.10.18.
 */
public class TransportApiResponseDecoder implements TbKafkaDecoder<TransportApiResponseMsg> {

    @Override
    public TransportApiResponseMsg decode(TbQueueMsg msg) throws IOException {
        return TransportApiResponseMsg.parseFrom(msg.getData());
    }
}
