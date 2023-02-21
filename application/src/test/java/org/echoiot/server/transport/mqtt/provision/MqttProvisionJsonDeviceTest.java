package org.echoiot.server.transport.mqtt.provision;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.device.provision.ProvisionResponseStatus;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.echoiot.server.transport.mqtt.MqttTestCallback;
import org.echoiot.server.transport.mqtt.MqttTestClient;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;

import java.util.concurrent.TimeUnit;

import static org.echoiot.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC;
import static org.echoiot.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttProvisionJsonDeviceTest extends AbstractMqttIntegrationTest {

    @Resource
    DeviceCredentialsService deviceCredentialsService;

    @Resource
    DeviceService deviceService;

    @Test
    public void testProvisioningDisabledDevice() throws Exception {
        processTestProvisioningDisabledDevice();
    }

    @Test
    public void testProvisioningCheckPreProvisionedDevice() throws Exception {
        processTestProvisioningCheckPreProvisionedDevice();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        processTestProvisioningCreateNewDeviceWithoutCredentials();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        processTestProvisioningCreateNewDeviceWithAccessToken();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithCert() throws Exception {
        processTestProvisioningCreateNewDeviceWithCert();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        processTestProvisioningCreateNewDeviceWithMqttBasic();
    }

    @Test
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    protected void processTestProvisioningDisabledDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.DISABLED)
                .build();
        super.processBeforeTest(configProperties);
        byte[] result = createMqttClientAndPublish();
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }


    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        byte[] result = createMqttClientAndPublish();
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    protected void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        @NotNull String requestCredentials = ",\"credentialsType\": \"ACCESS_TOKEN\",\"token\": \"test_token\"";
        byte[] result = createMqttClientAndPublish(requestCredentials);
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "ACCESS_TOKEN");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    protected void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        @NotNull String requestCredentials = ",\"credentialsType\": \"X509_CERTIFICATE\",\"hash\": \"testHash\"";
        byte[] result = createMqttClientAndPublish(requestCredentials);
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "X509_CERTIFICATE");

        @NotNull String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    protected void processTestProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        @NotNull String requestCredentials = ",\"credentialsType\": \"MQTT_BASIC\",\"clientId\": \"test_clientId\",\"username\": \"test_username\",\"password\": \"test_password\"";
        byte[] result = createMqttClientAndPublish(requestCredentials);
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "MQTT_BASIC");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), EncryptionUtil.getSha3Hash("|", "test_clientId", "test_username"));

        @NotNull BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
        mqttCredentials.setClientId("test_clientId");
        mqttCredentials.setUserName("test_username");
        mqttCredentials.setPassword("test_password");

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), JacksonUtil.toString(mqttCredentials));
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    protected void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        byte[] result = createMqttClientAndPublish();
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    protected void processTestProvisioningWithBadKeyDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKeyOrig")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        byte[] result = createMqttClientAndPublish();
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }

    protected byte[] createMqttClientAndPublish() throws Exception {
        return createMqttClientAndPublish("");
    }

    protected byte[] createMqttClientAndPublish(String deviceCredentials) throws Exception {
        @NotNull String provisionRequestMsg = createTestProvisionMessage(deviceCredentials);
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait("provision");
        @NotNull MqttTestCallback onProvisionCallback = new MqttTestCallback(DEVICE_PROVISION_RESPONSE_TOPIC);
        client.setCallback(onProvisionCallback);
        client.subscribe(DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE);
        client.publishAndWait(DEVICE_PROVISION_REQUEST_TOPIC, provisionRequestMsg.getBytes());
        onProvisionCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        client.disconnect();
        return onProvisionCallback.getPayloadBytes();
    }

    @NotNull
    protected String createTestProvisionMessage(String deviceCredentials) {
        return "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"" + deviceCredentials + "}";
    }
}
