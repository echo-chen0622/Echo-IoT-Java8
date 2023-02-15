package org.thingsboard.server.service.edge.rpc.constructor;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class DashboardMsgConstructor {

    public DashboardUpdateMsg constructDashboardUpdatedMsg(UpdateMsgType msgType, Dashboard dashboard) {
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

    public DashboardUpdateMsg constructDashboardDeleteMsg(DashboardId dashboardId) {
        return DashboardUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(dashboardId.getId().getMostSignificantBits())
                .setIdLSB(dashboardId.getId().getLeastSignificantBits()).build();
    }

}
