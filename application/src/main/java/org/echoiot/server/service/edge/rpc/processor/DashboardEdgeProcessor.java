package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.gen.edge.v1.DashboardUpdateMsg;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@TbCoreComponent
public class DashboardEdgeProcessor extends BaseEdgeProcessor {

    @Nullable
    public DownlinkMsg convertDashboardEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Dashboard dashboard = dashboardService.findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg =
                            dashboardMsgConstructor.constructDashboardUpdatedMsg(msgType, dashboard);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DashboardUpdateMsg dashboardUpdateMsg =
                        dashboardMsgConstructor.constructDashboardDeleteMsg(dashboardId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processDashboardNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
