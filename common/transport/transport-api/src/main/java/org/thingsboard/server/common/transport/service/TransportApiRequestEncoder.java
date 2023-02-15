package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.queue.kafka.TbKafkaEncoder;

/**
 * Created by ashvayka on 05.10.18.
 */
public class TransportApiRequestEncoder implements TbKafkaEncoder<TransportApiRequestMsg> {
    @Override
    public byte[] encode(TransportApiRequestMsg value) {
        return value.toByteArray();
    }
}
