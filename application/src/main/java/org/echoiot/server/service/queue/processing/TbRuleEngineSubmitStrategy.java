package org.echoiot.server.service.queue.processing;

import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

public interface TbRuleEngineSubmitStrategy {

    void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs);

    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getPendingMap();

    void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer);

    void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap);

    void onSuccess(UUID id);

    void stop();
}