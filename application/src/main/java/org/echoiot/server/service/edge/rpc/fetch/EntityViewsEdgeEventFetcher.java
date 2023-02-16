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

@AllArgsConstructor
@Slf4j
public class EntityViewsEdgeEventFetcher extends BasePageableEdgeEventFetcher<EntityView> {

    private final EntityViewService entityViewService;

    @Override
    PageData<EntityView> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, EntityView entityView) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                                            EdgeEventActionType.ADDED, entityView.getId(), null);
    }
}
