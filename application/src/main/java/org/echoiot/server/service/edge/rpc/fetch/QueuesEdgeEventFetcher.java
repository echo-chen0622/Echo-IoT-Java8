package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.dao.queue.QueueService;

@AllArgsConstructor
@Slf4j
public class QueuesEdgeEventFetcher extends BasePageableEdgeEventFetcher<Queue> {

    private final QueueService queueService;

    @Override
    PageData<Queue> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return queueService.findQueuesByTenantId(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Queue queue) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.QUEUE,
                                            EdgeEventActionType.ADDED, queue.getId(), null);
    }
}
