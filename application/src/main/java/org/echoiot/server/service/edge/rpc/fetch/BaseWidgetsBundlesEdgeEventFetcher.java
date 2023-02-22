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
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetsBundleService;

@Slf4j
@AllArgsConstructor
public abstract class BaseWidgetsBundlesEdgeEventFetcher extends BasePageableEdgeEventFetcher<WidgetsBundle> {

    protected final WidgetsBundleService widgetsBundleService;

    @Override
    PageData<WidgetsBundle> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return findWidgetsBundles(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, WidgetsBundle widgetsBundle) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGETS_BUNDLE,
                                            EdgeEventActionType.ADDED, widgetsBundle.getId(), null);
    }

    protected abstract PageData<WidgetsBundle> findWidgetsBundles(TenantId tenantId, PageLink pageLink);
}
