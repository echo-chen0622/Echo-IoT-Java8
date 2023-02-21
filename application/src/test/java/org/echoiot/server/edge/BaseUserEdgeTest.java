package org.echoiot.server.edge;

import com.google.protobuf.AbstractMessage;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.service.security.model.ChangePasswordRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseUserEdgeTest extends AbstractEdgeTest {

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    public void testCreateUpdateDeleteTenantUser() throws Exception {
        // create user
        edgeImitator.expectMessageAmount(2);
        @NotNull User newTenantAdmin = new User();
        newTenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        newTenantAdmin.setTenantId(savedTenant.getId());
        newTenantAdmin.setEmail("tenantAdmin@echoiot.org");
        newTenantAdmin.setFirstName("Boris");
        newTenantAdmin.setLastName("Johnson");
        User savedTenantAdmin = createUser(newTenantAdmin, "tenant");
        Assert.assertTrue(edgeImitator.waitForMessages()); // wait 2 messages - user update msg and user credentials update msg
        @NotNull Optional<UserUpdateMsg> latestMessageOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(latestMessageOpt.isPresent());
        @NotNull UserUpdateMsg userUpdateMsg = latestMessageOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
        Assert.assertEquals(savedTenantAdmin.getAuthority().name(), userUpdateMsg.getAuthority());
        Assert.assertEquals(savedTenantAdmin.getEmail(), userUpdateMsg.getEmail());
        Assert.assertEquals(savedTenantAdmin.getFirstName(), userUpdateMsg.getFirstName());
        Assert.assertEquals(savedTenantAdmin.getLastName(), userUpdateMsg.getLastName());
        @NotNull Optional<UserCredentialsUpdateMsg> userCredentialsUpdateMsgOpt = edgeImitator.findMessageByType(UserCredentialsUpdateMsg.class);
        Assert.assertTrue(userCredentialsUpdateMsgOpt.isPresent());

        // update user
        edgeImitator.expectMessageAmount(1);
        savedTenantAdmin.setLastName("Borisov");
        savedTenantAdmin = doPost("/api/user", savedTenantAdmin, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getLastName(), userUpdateMsg.getLastName());

        // update user credentials
        edgeImitator.expectMessageAmount(1);
        login(savedTenantAdmin.getEmail(), "tenant");
        @NotNull ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword("newTenant");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        @NotNull UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userCredentialsUpdateMsg.getUserIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userCredentialsUpdateMsg.getUserIdLSB());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsUpdateMsg.getPassword()));

        // delete user
        edgeImitator.expectMessageAmount(1);
        login(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/user/" + savedTenantAdmin.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
    }

    @Test
    public void testCreateUpdateDeleteCustomerUser() throws Exception {
        // create customer
        edgeImitator.expectMessageAmount(1);
        @NotNull Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // assign edge to customer
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // create user
        edgeImitator.expectMessageAmount(2);
        @NotNull User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customerUser@echoiot.org");
        customerUser.setFirstName("John");
        customerUser.setLastName("Edwards");
        User savedCustomerUser = createUser(customerUser, "customer");
        Assert.assertTrue(edgeImitator.waitForMessages());  // wait 2 messages - user update msg and user credentials update msg
        @NotNull Optional<UserUpdateMsg> latestMessageOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(latestMessageOpt.isPresent());
        @NotNull UserUpdateMsg userUpdateMsg = latestMessageOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomerUser.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
        Assert.assertEquals(savedCustomerUser.getCustomerId().getId().getMostSignificantBits(), userUpdateMsg.getCustomerIdMSB());
        Assert.assertEquals(savedCustomerUser.getCustomerId().getId().getLeastSignificantBits(), userUpdateMsg.getCustomerIdLSB());
        Assert.assertEquals(savedCustomerUser.getAuthority().name(), userUpdateMsg.getAuthority());
        Assert.assertEquals(savedCustomerUser.getEmail(), userUpdateMsg.getEmail());
        Assert.assertEquals(savedCustomerUser.getFirstName(), userUpdateMsg.getFirstName());
        Assert.assertEquals(savedCustomerUser.getLastName(), userUpdateMsg.getLastName());

        // update user
        edgeImitator.expectMessageAmount(1);
        savedCustomerUser.setLastName("Addams");
        savedCustomerUser = doPost("/api/user", savedCustomerUser, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getLastName(), userUpdateMsg.getLastName());

        // update user credentials
        edgeImitator.expectMessageAmount(1);
        login(savedCustomerUser.getEmail(), "customer");
        @NotNull ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("customer");
        changePasswordRequest.setNewPassword("newCustomer");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        @NotNull UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(savedCustomerUser.getUuidId().getMostSignificantBits(), userCredentialsUpdateMsg.getUserIdMSB());
        Assert.assertEquals(savedCustomerUser.getUuidId().getLeastSignificantBits(), userCredentialsUpdateMsg.getUserIdLSB());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsUpdateMsg.getPassword()));

        // delete user
        edgeImitator.expectMessageAmount(1);
        login(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/user/" + savedCustomerUser.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomerUser.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());

    }

    @Test
    public void testSendUserCredentialsRequestToCloud() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdmin.getId().getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdmin.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        @NotNull UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(tenantAdmin.getId().getId().getMostSignificantBits(), userCredentialsUpdateMsg.getUserIdMSB());
        Assert.assertEquals(tenantAdmin.getId().getId().getLeastSignificantBits(), userCredentialsUpdateMsg.getUserIdLSB());
    }

    @Test
    public void sendUserCredentialsRequest() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdmin.getId().getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdmin.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        @NotNull UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdMSB(), tenantAdmin.getId().getId().getMostSignificantBits());
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdLSB(), tenantAdmin.getId().getId().getLeastSignificantBits());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }
}
