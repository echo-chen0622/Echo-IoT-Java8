package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.edge.EdgeEventService;

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