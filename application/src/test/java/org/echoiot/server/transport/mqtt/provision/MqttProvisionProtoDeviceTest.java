package org.echoiot.server.transport.mqtt.provision;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.device.provision.ProvisionResponseStatus;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.echoiot.server.transport.mqtt.MqttTestCallback;
import org.echoiot.server.transport.mqtt.MqttTestClient;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static org.echoiot.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC;
import static org.echoiot.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttProvisionProtoDeviceTest extends AbstractMqttIntegrationTest {

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
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.DISABLED)
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg result = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());
        Assert.assertNotNull(result);
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), result.getStatus().name());
    }

    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().name());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().name());
    }

    protected void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder()
                .setValidateDeviceTokenRequestMsg(ValidateDeviceTokenRequestMsg.newBuilder().setToken("test_token").build())
                .build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.ACCESS_TOKEN, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.ACCESS_TOKEN);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder()
                .setValidateDeviceX509CertRequestMsg(
                        ValidateDeviceX509CertRequestMsg.newBuilder().setHash("testHash").build())
                .build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.X509_CERTIFICATE, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.X509_CERTIFICATE);

        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder().setValidateBasicMqttCredRequestMsg(
                ValidateBasicMqttCredRequestMsg.newBuilder()
                        .setClientId("test_clientId")
                        .setUserName("test_username")
                        .setPassword("test_password")
                    .build()
        ).build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.MQTT_BASIC, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.MQTT_BASIC);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), EncryptionUtil.getSha3Hash("|", "test_clientId", "test_username"));

        BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
        mqttCredentials.setClientId("test_clientId");
        mqttCredentials.setUserName("test_username");
        mqttCredentials.setPassword("test_password");

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), JacksonUtil.toString(mqttCredentials));
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().name());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().name());
    }

    protected void processTestProvisioningWithBadKeyDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKeyOrig")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.getStatus().name());
    }

    protected byte[] createMqttClientAndPublish() throws Exception {
        byte[] provisionRequestMsg = createTestProvisionMessage();
        return createMqttClientAndPublish(provisionRequestMsg);
    }

    protected byte[] createMqttClientAndPublish(byte[] provisionRequestMsg) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait("provision");
        MqttTestCallback onProvisionCallback = new MqttTestCallback(DEVICE_PROVISION_RESPONSE_TOPIC);
        client.setCallback(onProvisionCallback);
        client.subscribe(DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE);
        client.publishAndWait(DEVICE_PROVISION_REQUEST_TOPIC, provisionRequestMsg);
        onProvisionCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        client.disconnect();
        return onProvisionCallback.getPayloadBytes();
    }

    protected byte[] createTestsProvisionMessage(@Nullable CredentialsType credentialsType, @Nullable CredentialsDataProto credentialsData) throws Exception {
        return ProvisionDeviceRequestMsg.newBuilder()
                .setDeviceName("Test Provision device")
                .setCredentialsType(credentialsType != null ? credentialsType : CredentialsType.ACCESS_TOKEN)
                .setCredentialsDataProto(credentialsData != null ? credentialsData: CredentialsDataProto.newBuilder().build())
                .setProvisionDeviceCredentialsMsg(
                        ProvisionDeviceCredentialsMsg.newBuilder()
                                .setProvisionDeviceKey("testProvisionKey")
                                .setProvisionDeviceSecret("testProvisionSecret")
                ).build()
                .toByteArray();
    }


    protected byte[] createTestProvisionMessage() throws Exception {
        return createTestsProvisionMessage(null, null);
    }

}
