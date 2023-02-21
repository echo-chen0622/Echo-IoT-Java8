package org.echoiot.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.AbstractMessage;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.ShortCustomerInfo;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.gen.edge.v1.DashboardUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseDashboardEdgeTest extends AbstractEdgeTest {

    @Test
    public void testDashboards() throws Exception {
        // create dashboard and assign to edge
        edgeImitator.expectMessageAmount(1);
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        @NotNull DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);

        // update dashboard
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Updated Edge Test Dashboard");
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());

        // unassign dashboard from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());

        // delete dashboard - no messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // create dashboard #2 and assign to edge
        edgeImitator.expectMessageAmount(1);
        dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard #2");
        savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());

        // assign dashboard #2 to customer
        @NotNull Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        @Nullable Set<ShortCustomerInfo> assignedCustomers =
                JacksonUtil.fromString(dashboardUpdateMsg.getAssignedCustomers(), new TypeReference<>() {});
        Assert.assertNotNull(assignedCustomers);
        Assert.assertFalse(assignedCustomers.isEmpty());
        Assert.assertTrue(assignedCustomers.contains(new ShortCustomerInfo(savedCustomer.getId(), customer.getTitle(), customer.isPublic())));

        // unassign dashboard #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        assignedCustomers =
                JacksonUtil.fromString(dashboardUpdateMsg.getAssignedCustomers(), new TypeReference<>() {});
        Assert.assertNotNull(assignedCustomers);
        Assert.assertTrue(assignedCustomers.isEmpty());

        // delete dashboard #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
    }

}
