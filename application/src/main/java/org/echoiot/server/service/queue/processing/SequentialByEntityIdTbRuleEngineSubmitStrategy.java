package org.echoiot.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

@Slf4j
public abstract class SequentialByEntityIdTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;
    private final ConcurrentMap<UUID, EntityId> msgToEntityIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>>> entityIdToListMap = new ConcurrentHashMap<>();

    public SequentialByEntityIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    public void init(@NotNull List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        super.init(msgs);
        initMaps();
    }

    @Override
    public void submitAttempt(@NotNull BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        entityIdToListMap.forEach((entityId, queue) -> {
            IdMsgPair<TransportProtos.ToRuleEngineMsg> msg = queue.peek();
            if (msg != null) {
                msgConsumer.accept(msg.uuid, msg.msg);
            }
        });
    }

    @Override
    public void update(@NotNull ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
        initMaps();
    }

    @Override
    protected void doOnSuccess(UUID id) {
        EntityId entityId = msgToEntityIdMap.get(id);
        if (entityId != null) {
            Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>> queue = entityIdToListMap.get(entityId);
            if (queue != null) {
                @Nullable IdMsgPair<TransportProtos.ToRuleEngineMsg> next = null;
                synchronized (queue) {
                    IdMsgPair<TransportProtos.ToRuleEngineMsg> expected = queue.peek();
                    if (expected != null && expected.uuid.equals(id)) {
                        queue.poll();
                        next = queue.peek();
                    }
                }
                if (next != null) {
                    msgConsumer.accept(next.uuid, next.msg);
                }
            }
        }
    }

    private void initMaps() {
        msgToEntityIdMap.clear();
        entityIdToListMap.clear();
        for (@NotNull IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            EntityId entityId = getEntityId(pair.msg.getValue());
            if (entityId != null) {
                msgToEntityIdMap.put(pair.uuid, entityId);
                entityIdToListMap.computeIfAbsent(entityId, id -> new LinkedList<>()).add(pair);
            }
        }
    }

    protected abstract EntityId getEntityId(TransportProtos.ToRuleEngineMsg msg);

}
