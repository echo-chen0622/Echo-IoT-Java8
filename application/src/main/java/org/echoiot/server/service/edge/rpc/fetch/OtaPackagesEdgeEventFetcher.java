package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.ota.OtaPackageService;

@AllArgsConstructor
@Slf4j
public class OtaPackagesEdgeEventFetcher extends BasePageableEdgeEventFetcher<OtaPackageInfo> {

    private final OtaPackageService otaPackageService;

    @Override
    PageData<OtaPackageInfo> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return otaPackageService.findTenantOtaPackagesByTenantId(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, OtaPackageInfo otaPackageInfo) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.OTA_PACKAGE,
                                            EdgeEventActionType.ADDED, otaPackageInfo.getId(), null);
    }
}
