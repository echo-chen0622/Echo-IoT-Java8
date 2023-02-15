package org.thingsboard.server.transport.mqtt.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.MqttTestClient;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@Slf4j
@DaoSqlTest
public class MqttClaimProtoDeviceTest extends MqttClaimDeviceTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
        createCustomerAndUser();
    }

    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Test
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device Proto", false);
    }

    @Test
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload Proto", true);
    }

    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = getClaimDevice(0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getClaimDevice(60000, emptyPayload).toByteArray();
        }
        byte[] failurePayloadBytes = getClaimDevice(1, emptyPayload).toByteArray();
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        processProtoTestGatewayClaimDevice(deviceName, emptyPayload);
    }

    private TransportApiProtos.ClaimDevice getClaimDevice(long duration, boolean emptyPayload) {
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setSecretKey("value");
            claimDeviceBuilder.setDurationMs(duration);
        }
        return claimDeviceBuilder.build();
    }


}
