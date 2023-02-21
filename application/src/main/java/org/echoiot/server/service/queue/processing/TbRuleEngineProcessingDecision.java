package org.echoiot.server.service.queue.processing;

import lombok.Data;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Data
public class TbRuleEngineProcessingDecision {

    private final boolean commit;
    @NotNull
    private final ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> reprocessMap;

}
