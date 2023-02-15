package org.thingsboard.server.dao.queue;

import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;

import java.util.List;

public interface QueueService {

    Queue saveQueue(Queue queue);

    void deleteQueue(TenantId tenantId, QueueId queueId);

    List<Queue> findQueuesByTenantId(TenantId tenantId);

    PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink);

    List<Queue> findAllQueues();

    Queue findQueueById(TenantId tenantId, QueueId queueId);

    Queue findQueueByTenantIdAndName(TenantId tenantId, String name);

    Queue findQueueByTenantIdAndNameInternal(TenantId tenantId, String queueName);

    void deleteQueuesByTenantId(TenantId tenantId);
}
