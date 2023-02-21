package org.echoiot.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BasePageableEdgeEventFetcher<T> implements EdgeEventFetcher {

    @NotNull
    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @NotNull
    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) {
        log.trace("[{}] start fetching edge events [{}]", tenantId, edge.getId());
        PageData<T> pageData = fetchPageData(tenantId, edge, pageLink);
        @NotNull List<EdgeEvent> result = new ArrayList<>();
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
