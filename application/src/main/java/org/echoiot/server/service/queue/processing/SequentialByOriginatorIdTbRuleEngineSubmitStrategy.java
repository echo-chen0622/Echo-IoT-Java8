package org.echoiot.server.service.queue.processing;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.msg.gen.MsgProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Slf4j
public class SequentialByOriginatorIdTbRuleEngineSubmitStrategy extends SequentialByEntityIdTbRuleEngineSubmitStrategy {

    public SequentialByOriginatorIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    protected EntityId getEntityId(@NotNull TransportProtos.ToRuleEngineMsg msg) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(msg.getTbMsg());
            return EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        } catch (InvalidProtocolBufferException e) {
            log.warn("[{}] Failed to parse TbMsg: {}", queueName, msg);
            return null;
        }
    }
}
