package org.thingsboard.server.service.queue.processing;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

@Slf4j
public class SequentialByOriginatorIdTbRuleEngineSubmitStrategy extends SequentialByEntityIdTbRuleEngineSubmitStrategy {

    public SequentialByOriginatorIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    protected EntityId getEntityId(TransportProtos.ToRuleEngineMsg msg) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(msg.getTbMsg());
            return EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        } catch (InvalidProtocolBufferException e) {
            log.warn("[{}] Failed to parse TbMsg: {}", queueName, msg);
            return null;
        }
    }
}
