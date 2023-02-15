package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.kafka.TbKafkaEncoder;

/**
 * Created by ashvayka on 05.10.18.
 */
public class ToRuleEngineMsgEncoder implements TbKafkaEncoder<ToRuleEngineMsg> {
    @Override
    public byte[] encode(ToRuleEngineMsg value) {
        return value.toByteArray();
    }
}
