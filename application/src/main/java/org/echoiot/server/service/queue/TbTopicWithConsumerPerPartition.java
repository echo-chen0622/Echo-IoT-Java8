package org.echoiot.server.service.queue;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Data
public class TbTopicWithConsumerPerPartition {
    @NotNull
    private final String topic;
    @Getter
    private final ReentrantLock lock = new ReentrantLock(); //NonfairSync
    @NotNull
    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private final ConcurrentMap<TopicPartitionInfo, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>>> consumers = new ConcurrentHashMap<>();
    private final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();
}
