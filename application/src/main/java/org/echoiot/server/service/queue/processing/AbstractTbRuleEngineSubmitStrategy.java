package org.echoiot.server.service.queue.processing;

import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public abstract class AbstractTbRuleEngineSubmitStrategy implements TbRuleEngineSubmitStrategy {

    protected final String queueName;
    protected List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> orderedMsgList;
    private volatile boolean stopped;

    public AbstractTbRuleEngineSubmitStrategy(String queueName) {
        this.queueName = queueName;
    }

    protected abstract void doOnSuccess(UUID id);

    @Override
    public void init(@NotNull List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getPendingMap() {
        return orderedMsgList.stream().collect(Collectors.toConcurrentMap(pair -> pair.uuid, pair -> pair.msg));
    }

    @Override
    public void update(@NotNull ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        @NotNull List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> newOrderedMsgList = new ArrayList<>(reprocessMap.size());
        for (@NotNull IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            if (reprocessMap.containsKey(pair.uuid)) {
                newOrderedMsgList.add(pair);
            }
        }
        orderedMsgList = newOrderedMsgList;
    }

    @Override
    public void onSuccess(UUID id) {
        if (!stopped) {
            doOnSuccess(id);
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }
}
