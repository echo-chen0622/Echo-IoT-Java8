package org.echoiot.server.service.queue.processing;

import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SequentialByTenantIdTbRuleEngineSubmitStrategy extends SequentialByEntityIdTbRuleEngineSubmitStrategy {

    public SequentialByTenantIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    protected EntityId getEntityId(@NotNull TransportProtos.ToRuleEngineMsg msg) {
        return TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
    }
}
