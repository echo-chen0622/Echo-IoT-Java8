package org.echoiot.server.service.edge.rpc;

import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.service.edge.EdgeContextComponent;
import org.echoiot.server.service.edge.rpc.fetch.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class EdgeSyncCursor {

    @NotNull
    List<EdgeEventFetcher> fetchers = new LinkedList<>();

    int currentIdx = 0;

    public EdgeSyncCursor(@NotNull EdgeContextComponent ctx, @NotNull Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), ctx.getFreemarkerConfig()));
            fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
            fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                fetchers.add(new CustomerEdgeEventFetcher());
                fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
            }
        }
        fetchers.add(new DevicesEdgeEventFetcher(ctx.getDeviceService()));
        fetchers.add(new AssetsEdgeEventFetcher(ctx.getAssetService()));
        fetchers.add(new EntityViewsEdgeEventFetcher(ctx.getEntityViewService()));
        fetchers.add(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));
        if (fullSync) {
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
        }
    }

    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

    public int getCurrentIdx() {
        return currentIdx;
    }
}
