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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class CustomerEdgeEventFetcher implements EdgeEventFetcher {

    @Nullable
    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        result.add(EdgeUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(),
                                                EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getCustomerId(), null));
        // @voba - returns PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }
}
