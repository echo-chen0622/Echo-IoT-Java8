package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Slf4j
public class AssetProfilesEdgeEventFetcher extends BasePageableEdgeEventFetcher<AssetProfile> {

    @NotNull
    private final AssetProfileService assetProfileService;

    @Override
    PageData<AssetProfile> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return assetProfileService.findAssetProfiles(tenantId, pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull AssetProfile assetProfile) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET_PROFILE,
                                            EdgeEventActionType.ADDED, assetProfile.getId(), null);
    }
}
