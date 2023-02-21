package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DashboardInfo;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Slf4j
public class DashboardsEdgeEventFetcher extends BasePageableEdgeEventFetcher<DashboardInfo> {

    @NotNull
    private final DashboardService dashboardService;

    @Override
    PageData<DashboardInfo> fetchPageData(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) {
        return dashboardService.findDashboardsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull DashboardInfo dashboardInfo) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DASHBOARD,
                                            EdgeEventActionType.ADDED, dashboardInfo.getId(), null);
    }
}
