package org.echoiot.server.service.edge.rpc;

import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.service.edge.EdgeContextComponent;
import org.echoiot.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.DevicesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.EntityViewsEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.echoiot.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class EdgeSyncCursor {

    List<EdgeEventFetcher> fetchers = new LinkedList<>();

    int currentIdx = 0;

    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
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
