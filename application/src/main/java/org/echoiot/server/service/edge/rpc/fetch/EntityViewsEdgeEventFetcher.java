package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Slf4j
public class EntityViewsEdgeEventFetcher extends BasePageableEdgeEventFetcher<EntityView> {

    @NotNull
    private final EntityViewService entityViewService;

    @Override
    PageData<EntityView> fetchPageData(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) {
        return entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull EntityView entityView) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                                            EdgeEventActionType.ADDED, entityView.getId(), null);
    }
}
