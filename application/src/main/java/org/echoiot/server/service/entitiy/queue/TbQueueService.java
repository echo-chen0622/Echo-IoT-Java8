package org.echoiot.server.service.entitiy.queue;

import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;

import java.util.List;

public interface TbQueueService {

    Queue saveQueue(Queue queue);

    void deleteQueue(TenantId tenantId, QueueId queueId);

    void deleteQueueByQueueName(TenantId tenantId, String queueName);

    void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile oldTenantProfile);
}
