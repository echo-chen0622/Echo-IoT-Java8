package org.echoiot.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Optional;
import java.util.UUID;

abstract public class BaseEdgeTest extends AbstractEdgeTest {

    @Test
    public void testEdge_assignToCustomer_unassignFromCustomer() throws Exception {
        // assign edge to customer
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // assign edge to customer
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<EdgeConfiguration> edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        EdgeConfiguration edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), edgeConfiguration.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), edgeConfiguration.getCustomerIdLSB());
        Optional<CustomerUpdateMsg> customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        CustomerUpdateMsg customerUpdateMsg = customerUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
        Assert.assertEquals(savedCustomer.getTitle(), customerUpdateMsg.getTitle());

        // unassign edge from customer
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/customer/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB())));
        customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        customerUpdateMsg = customerUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
    }
}
