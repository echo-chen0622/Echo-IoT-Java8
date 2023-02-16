package org.echoiot.server.common.transport.service;

import org.echoiot.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.echoiot.server.queue.kafka.TbKafkaEncoder;

/**
 * Created by Echo on 05.10.18.
 */
public class TransportApiRequestEncoder implements TbKafkaEncoder<TransportApiRequestMsg> {
    @Override
    public byte[] encode(TransportApiRequestMsg value) {
        return value.toByteArray();
    }
}
