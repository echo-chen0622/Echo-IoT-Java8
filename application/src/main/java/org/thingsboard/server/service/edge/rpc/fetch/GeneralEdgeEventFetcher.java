package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.EdgeEventService;

@AllArgsConstructor
public class GeneralEdgeEventFetcher implements EdgeEventFetcher {

    private final Long queueStartTs;
    private final EdgeEventService edgeEventService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new TimePageLink(
                pageSize,
                0,
                null,
                new SortOrder("createdTime", SortOrder.Direction.ASC),
                queueStartTs,
                null);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        return edgeEventService.findEdgeEvents(tenantId, edge.getId(), (TimePageLink) pageLink, true);
    }
}
