package org.thingsboard.server.queue.discovery;

import lombok.Data;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueUpdateMsg;

import java.util.UUID;


@Data
public class QueueRoutingInfo {

    private final TenantId tenantId;
    private final QueueId queueId;
    private final String queueName;
    private final String queueTopic;
    private final int partitions;

    public QueueRoutingInfo(Queue queue) {
        this.tenantId = queue.getTenantId();
        this.queueId = queue.getId();
        this.queueName = queue.getName();
        this.queueTopic = queue.getTopic();
        this.partitions = queue.getPartitions();
    }

    public QueueRoutingInfo(GetQueueRoutingInfoResponseMsg routingInfo) {
        this.tenantId = new TenantId(new UUID(routingInfo.getTenantIdMSB(), routingInfo.getTenantIdLSB()));
        this.queueId = new QueueId(new UUID(routingInfo.getQueueIdMSB(), routingInfo.getQueueIdLSB()));
        this.queueName = routingInfo.getQueueName();
        this.queueTopic = routingInfo.getQueueTopic();
        this.partitions = routingInfo.getPartitions();
    }

    public QueueRoutingInfo(QueueUpdateMsg queueUpdateMsg) {
        this.tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        this.queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
        this.queueName = queueUpdateMsg.getQueueName();
        this.queueTopic = queueUpdateMsg.getQueueTopic();
        this.partitions = queueUpdateMsg.getPartitions();
    }
}
