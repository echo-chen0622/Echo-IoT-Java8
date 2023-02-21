package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.gen.edge.v1.DashboardUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class DashboardMsgConstructor {

    @NotNull
    public DashboardUpdateMsg constructDashboardUpdatedMsg(UpdateMsgType msgType, @NotNull Dashboard dashboard) {
        DashboardUpdateMsg.Builder builder = DashboardUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(dashboard.getId().getId().getMostSignificantBits())
                .setIdLSB(dashboard.getId().getId().getLeastSignificantBits())
                .setTitle(dashboard.getTitle())
                .setConfiguration(JacksonUtil.toString(dashboard.getConfiguration()));
        if (dashboard.getAssignedCustomers() != null) {
            builder.setAssignedCustomers(JacksonUtil.toString(dashboard.getAssignedCustomers()));
        }
        return builder.build();
    }

    @NotNull
    public DashboardUpdateMsg constructDashboardDeleteMsg(@NotNull DashboardId dashboardId) {
        return DashboardUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(dashboardId.getId().getMostSignificantBits())
                .setIdLSB(dashboardId.getId().getLeastSignificantBits()).build();
    }

}
