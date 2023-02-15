package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BasePageableEdgeEventFetcher<T> implements EdgeEventFetcher {

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        log.trace("[{}] start fetching edge events [{}]", tenantId, edge.getId());
        PageData<T> pageData = fetchPageData(tenantId, edge, pageLink);
        List<EdgeEvent> result = new ArrayList<>();
        if (!pageData.getData().isEmpty()) {
            for (T entity : pageData.getData()) {
                result.add(constructEdgeEvent(tenantId, edge, entity));
            }
        }
        return new PageData<>(result, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    abstract PageData<T> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink);

    abstract EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, T entity);
}
