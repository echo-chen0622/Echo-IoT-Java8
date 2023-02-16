package org.echoiot.server.dao.queue;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.dao.Dao;

import java.util.List;

public interface QueueDao extends Dao<Queue> {
    Queue findQueueByTenantIdAndTopic(TenantId tenantId, String topic);

    Queue findQueueByTenantIdAndName(TenantId tenantId, String name);

    List<Queue> findAllMainQueues();

    List<Queue> findAllQueues();

    List<Queue> findAllByTenantId(TenantId tenantId);

    PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink);
}
