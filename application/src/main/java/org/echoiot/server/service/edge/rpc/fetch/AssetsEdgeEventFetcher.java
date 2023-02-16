package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.asset.AssetService;

@AllArgsConstructor
@Slf4j
public class AssetsEdgeEventFetcher extends BasePageableEdgeEventFetcher<Asset> {

    private final AssetService assetService;

    @Override
    PageData<Asset> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return assetService.findAssetsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Asset asset) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET,
                                            EdgeEventActionType.ADDED, asset.getId(), null);
    }
}
