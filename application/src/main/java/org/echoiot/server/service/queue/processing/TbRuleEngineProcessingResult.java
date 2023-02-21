package org.echoiot.server.service.queue.processing;

import lombok.Getter;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.echoiot.server.service.queue.TbMsgPackProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private final String queueName;
    @Getter
    private final boolean success;
    @Getter
    private final boolean timeout;
    @Getter
    private final TbMsgPackProcessingContext ctx;

    public TbRuleEngineProcessingResult(String queueName, boolean timeout, @NotNull TbMsgPackProcessingContext ctx) {
        this.queueName = queueName;
        this.timeout = timeout;
        this.ctx = ctx;
        this.success = !timeout && ctx.getPendingMap().isEmpty() && ctx.getFailedMap().isEmpty();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getPendingMap() {
        return ctx.getPendingMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getSuccessMap() {
        return ctx.getSuccessMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getFailedMap() {
        return ctx.getFailedMap();
    }

    public ConcurrentMap<TenantId, RuleEngineException> getExceptionsMap() {
        return ctx.getExceptionsMap();
    }
}
