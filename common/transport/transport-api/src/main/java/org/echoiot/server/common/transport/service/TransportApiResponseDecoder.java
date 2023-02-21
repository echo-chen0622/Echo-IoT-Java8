package org.echoiot.server.common.transport.service;

import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.echoiot.server.queue.kafka.TbKafkaDecoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by Echo on 05.10.18.
 */
public class TransportApiResponseDecoder implements TbKafkaDecoder<TransportApiResponseMsg> {

    @Override
    public TransportApiResponseMsg decode(@NotNull TbQueueMsg msg) throws IOException {
        return TransportApiResponseMsg.parseFrom(msg.getData());
    }
}
