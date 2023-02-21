package org.echoiot.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.protobuf.AbstractMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.awaitility.Awaitility;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.mqtt.MqttTestCallback;
import org.echoiot.server.transport.mqtt.MqttTestClient;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "transport.mqtt.enabled=true"
})
abstract public class BaseDeviceEdgeTest extends AbstractEdgeTest {

    @Test
    public void testDevices() throws Exception {
        // create device and assign to edge; update device
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // unassign device from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        @NotNull DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        // delete device - no messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // create device #2 and assign to edge
        edgeImitator.expectMessageAmount(2);
        savedDevice = saveDevice("Edge Device 3", "Default");
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        @NotNull Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());

        @NotNull Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        @NotNull DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getDeviceProfileId().getId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getDeviceProfileId().getId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        // assign device #2 to customer
        @NotNull Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getCustomerIdLSB());

        // unassign device #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB())));

        // delete device #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

    }

    @Test
    public void testUpdateDeviceCredentials() throws Exception {
        // create device and assign to edge; update device
        @NotNull Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // update device credentials - ACCESS_TOKEN
        edgeImitator.expectMessageAmount(1);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        @NotNull DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), deviceCredentialsUpdateMsg.getCredentialsType());
        Assert.assertEquals(deviceCredentials.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsId());
        Assert.assertFalse(deviceCredentialsUpdateMsg.hasCredentialsValue());

        // update device credentials - X509_CERTIFICATE
        edgeImitator.expectMessageAmount(1);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        deviceCredentials.setCredentialsId(null);
        deviceCredentials.setCredentialsValue("-----BEGIN RSA PRIVATE KEY-----");
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), deviceCredentialsUpdateMsg.getCredentialsType());
        Assert.assertFalse(deviceCredentialsUpdateMsg.getCredentialsId().isEmpty());
        Assert.assertTrue(deviceCredentialsUpdateMsg.hasCredentialsValue());
        Assert.assertEquals(deviceCredentials.getCredentialsValue(), deviceCredentialsUpdateMsg.getCredentialsValue());
    }

    @Test
    public void testDeviceReachedMaximumAllowedOnCloud() throws Exception {
        // update tenant profile configuration
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + savedTenant.getTenantProfileId().getId(), TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        profileConfiguration.setMaxDevices(1);
        tenantProfile.getProfileData().setConfiguration(profileConfiguration);
        doPost("/api/tenantProfile/", tenantProfile, TenantProfile.class);

        loginTenantAdmin();

        @NotNull UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device");
        deviceUpdateMsgBuilder.setType("default");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());
    }

    @Test
    public void testSendDeviceRpcResponseToCloud() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceRpcCallMsg.Builder deviceRpcCallResponseBuilder = DeviceRpcCallMsg.newBuilder();
        deviceRpcCallResponseBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceRpcCallResponseBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceRpcCallResponseBuilder.setOneway(true);
        deviceRpcCallResponseBuilder.setRequestId(0);
        deviceRpcCallResponseBuilder.setExpirationTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        RpcResponseMsg.Builder responseBuilder =
                RpcResponseMsg.newBuilder().setResponse("{}");
        testAutoGeneratedCodeByProtobuf(responseBuilder);

        deviceRpcCallResponseBuilder.setResponseMsg(responseBuilder.build());
        testAutoGeneratedCodeByProtobuf(deviceRpcCallResponseBuilder);

        uplinkMsgBuilder.addDeviceRpcCallMsg(deviceRpcCallResponseBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    @Test
    public void testSendDeviceCredentialsUpdateToCloud() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsUpdateMsg.Builder deviceCredentialsUpdateMsgBuilder = DeviceCredentialsUpdateMsg.newBuilder();
        deviceCredentialsUpdateMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN.name());
        deviceCredentialsUpdateMsgBuilder.setCredentialsId("NEW_TOKEN");
        testAutoGeneratedCodeByProtobuf(deviceCredentialsUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    @Test
    public void testSendDeviceCredentialsRequestToCloud() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getUuidId() + "/credentials", DeviceCredentials.class);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsRequestMsg.Builder deviceCredentialsRequestMsgBuilder = DeviceCredentialsRequestMsg.newBuilder();
        deviceCredentialsRequestMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsRequestMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        @NotNull DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsType(), deviceCredentials.getCredentialsType().name());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentials.getCredentialsId());
    }

    @Test
    public void testSendAttributesRequestToCloud() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"key1\":\"value1\"}",
                "key1", "value1");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"inactivityTimeout\":3600000}",
                "inactivityTimeout", "3600000");
        sendAttributesRequestAndVerify(device, DataConstants.SHARED_SCOPE, "{\"key2\":\"value2\"}",
                "key2", "value2");
    }

    @Test
    public void testSendDeleteDeviceOnEdgeToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceDeleteMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        deviceDeleteMsgBuilder.setIdMSB(device.getId().getId().getMostSignificantBits());
        deviceDeleteMsgBuilder.setIdLSB(device.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceDeleteMsgBuilder);

        upLinkMsgBuilder.addDeviceUpdateMsg(deviceDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        device = doGet("/api/device/" + device.getUuidId(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeDevices.contains(device));
    }

    @Test
    public void testSendTelemetryToCloud() throws Exception {
        @NotNull Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        edgeImitator.expectResponsesAmount(2);

        @NotNull JsonObject data = new JsonObject();
        @NotNull String timeseriesKey = "key";
        @NotNull String timeseriesValue = "25";
        data.addProperty(timeseriesKey, timeseriesValue);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data, System.currentTimeMillis()));
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(entityDataBuilder);
        uplinkMsgBuilder.addEntityData(entityDataBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        @NotNull JsonObject attributesData = new JsonObject();
        @NotNull String attributesKey = "test_attr";
        @NotNull String attributesValue = "test_value";
        attributesData.addProperty(attributesKey, attributesValue);
        UplinkMsg.Builder uplinkMsgBuilder2 = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder2 = EntityDataProto.newBuilder();
        entityDataBuilder2.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder2.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder2.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder2.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder2.setPostAttributeScope(DataConstants.SERVER_SCOPE);

        uplinkMsgBuilder2.addEntityData(entityDataBuilder2.build());

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder2.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> loadDeviceTimeseries(device, timeseriesKey).containsKey(timeseriesKey));

        Map<String, List<Map<String, String>>> timeseries = loadDeviceTimeseries(device, timeseriesKey);
        Assert.assertTrue(timeseries.containsKey(timeseriesKey));
        Assert.assertEquals(1, timeseries.get(timeseriesKey).size());
        Assert.assertEquals(timeseriesValue, timeseries.get(timeseriesKey).get(0).get("value"));

        @NotNull String attributeValuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/" + DataConstants.SERVER_SCOPE;
        List<Map<String, String>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {});

        Assert.assertEquals(3, attributes.size());

        @NotNull Optional<Map<String, String>> activeAttributeOpt = getAttributeByKey("active", attributes);
        Assert.assertTrue(activeAttributeOpt.isPresent());
        @NotNull Map<String, String> activeAttribute = activeAttributeOpt.get();
        Assert.assertEquals("true", activeAttribute.get("value"));

        @NotNull Optional<Map<String, String>> customAttributeOpt = getAttributeByKey(attributesKey, attributes);
        Assert.assertTrue(customAttributeOpt.isPresent());
        @NotNull Map<String, String> customAttribute = customAttributeOpt.get();
        Assert.assertEquals(attributesValue, customAttribute.get("value"));

        doDelete("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/SERVER_SCOPE?keys=" + attributesKey, String.class);
    }

    @Test
    public void testSendDeviceToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        @NotNull String deviceOnCloudName = StringUtils.randomAlphanumeric(15);
        Device deviceOnCloud = saveDevice(deviceOnCloudName, "Default");

        @NotNull UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName(deviceOnCloudName);
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(2);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        @NotNull Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        @NotNull DeviceUpdateMsg latestDeviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertNotEquals(deviceOnCloudName, latestDeviceUpdateMsg.getName());
        Assert.assertEquals(deviceOnCloudName, latestDeviceUpdateMsg.getConflictName());

        @NotNull UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Assert.assertNotEquals(deviceOnCloud.getId().getId(), newDeviceId);

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());

        @NotNull Optional<DeviceCredentialsRequestMsg> deviceCredentialsUpdateMsgOpt = edgeImitator.findMessageByType(DeviceCredentialsRequestMsg.class);
        Assert.assertTrue(deviceCredentialsUpdateMsgOpt.isPresent());
        @NotNull DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = deviceCredentialsUpdateMsgOpt.get();
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());
    }

    @Test
    public void testSendDeviceToCloud() throws Exception {
        @NotNull UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device 2");
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsRequestMsg);
        @NotNull DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = (DeviceCredentialsRequestMsg) latestMessage;
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        @NotNull UUID newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals("Edge Device 2", device.getName());
    }

    @Test
    public void testRpcCall() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");

        ObjectNode body = mapper.createObjectNode();
        body.put("requestId", new Random().nextInt());
        body.put("requestUUID", Uuids.timeBased().toString());
        body.put("oneway", false);
        body.put("expirationTime", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        body.put("method", "test_method");
        body.put("params", "{\"param1\":\"value1\"}");
        body.put("persisted", true);
        body.put("retries", 2);

        @NotNull EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.RPC_CALL,
                                                          device.getId().getId(), EdgeEventType.DEVICE, body);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceRpcCallMsg);
        @NotNull DeviceRpcCallMsg latestDeviceRpcCallMsg = (DeviceRpcCallMsg) latestMessage;
        Assert.assertEquals("test_method", latestDeviceRpcCallMsg.getRequestMsg().getMethod());
        Assert.assertTrue(latestDeviceRpcCallMsg.getPersisted());
        Assert.assertEquals(2, latestDeviceRpcCallMsg.getRetries());
    }

    private void sendAttributesRequestAndVerify(@NotNull Device device, String scope, String attributesDataStr, String expectedKey,
                                                @NotNull String expectedValue) throws Exception {
        JsonNode attributesData = mapper.readTree(attributesDataStr);

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + scope,
                attributesData);

        // Wait before device attributes saved to database before requesting them from edge
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    @NotNull String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/attributes/" + scope;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains(expectedKey);
                });

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AttributesRequestMsg.Builder attributesRequestMsgBuilder = AttributesRequestMsg.newBuilder();
        attributesRequestMsgBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        attributesRequestMsgBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        attributesRequestMsgBuilder.setEntityType(EntityType.DEVICE.name());
        attributesRequestMsgBuilder.setScope(scope);
        testAutoGeneratedCodeByProtobuf(attributesRequestMsgBuilder);
        uplinkMsgBuilder.addAttributesRequestMsg(attributesRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        @NotNull EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals(scope, latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        @NotNull TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();

        boolean found = false;
        for (@NotNull TransportProtos.KeyValueProto keyValueProto : attributesUpdatedMsg.getKvList()) {
            if (keyValueProto.getKey().equals(expectedKey)) {
                Assert.assertEquals(expectedKey, keyValueProto.getKey());
                switch (keyValueProto.getType()) {
                    case STRING_V:
                        Assert.assertEquals(expectedValue, keyValueProto.getStringV());
                        break;
                    case LONG_V:
                        Assert.assertEquals(Long.parseLong(expectedValue), keyValueProto.getLongV());
                        break;
                    default:
                        Assert.fail("Unexpected data type: " + keyValueProto.getType());
                }
                found = true;
            }
        }
        Assert.assertTrue("Expected key and value must be found", found);
    }

    @NotNull
    private Optional<Map<String, String>> getAttributeByKey(String key, @NotNull List<Map<String, String>> attributes) {
        return attributes.stream().filter(kv -> kv.get("key").equals(key)).findFirst();
    }

    private Map<String, List<Map<String, String>>> loadDeviceTimeseries(@NotNull Device device, String timeseriesKey) throws Exception {
        return doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/timeseries?keys=" + timeseriesKey,
                new TypeReference<>() {});
    }

    @Test
    public void sendUpdateSharedAttributeToCloudAndValidateDeviceSubscription() throws Exception {
        @NotNull Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getUuidId() + "/credentials", DeviceCredentials.class);

        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(deviceCredentials.getCredentialsId());
        @NotNull MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);
        client.subscribeAndWait("v1/devices/me/attributes", MqttQoS.AT_MOST_ONCE);

        edgeImitator.expectResponsesAmount(1);

        @NotNull JsonObject attributesData = new JsonObject();
        @NotNull String attrKey = "sharedAttrName";
        @NotNull String attrValue = "sharedAttrValue";
        attributesData.addProperty(attrKey, attrValue);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder.setPostAttributeScope(DataConstants.SHARED_SCOPE);
        uplinkMsgBuilder.addEntityData(entityDataBuilder.build());

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Assert.assertTrue(onUpdateCallback.getSubscribeLatch().await(5, TimeUnit.SECONDS));

        Assert.assertEquals(JacksonUtil.OBJECT_MAPPER.createObjectNode().put(attrKey, attrValue),
                JacksonUtil.fromBytes(onUpdateCallback.getPayloadBytes()));

        client.disconnect();
    }
}
