package org.echoiot.server.common.transport.service;

import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.echoiot.server.queue.kafka.TbKafkaEncoder;

/**
 * Created by Echo on 05.10.18.
 */
public class ToRuleEngineMsgEncoder implements TbKafkaEncoder<ToRuleEngineMsg> {
    @Override
    public byte[] encode(ToRuleEngineMsg value) {
        return value.toByteArray();
    }
}
