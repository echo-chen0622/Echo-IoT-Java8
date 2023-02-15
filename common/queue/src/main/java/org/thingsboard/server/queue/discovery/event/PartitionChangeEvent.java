package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;

@ToString(callSuper = true)
public class PartitionChangeEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -8731788167026510559L;

    @Getter
    private final QueueKey queueKey;
    @Getter
    private final Set<TopicPartitionInfo> partitions;

    public PartitionChangeEvent(Object source, QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        super(source);
        this.queueKey = queueKey;
        this.partitions = partitions;
    }

    public ServiceType getServiceType() {
        return queueKey.getType();
    }
}
