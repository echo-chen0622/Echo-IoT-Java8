package org.echoiot.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestPropertySource;
import org.echoiot.server.common.data.CoapDeviceType;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceProfileInfo;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.echoiot.server.common.data.DeviceProfileType;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.echoiot.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.echoiot.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.echoiot.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.transport.AbstractTransportIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "transport.coap.enabled=true",
})
@Slf4j
public abstract class AbstractCoapIntegrationTest extends AbstractTransportIntegrationTest {

    protected final byte[] EMPTY_PAYLOAD = new byte[0];
    protected CoapTestClient client;

    protected void processAfterTest() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    protected void processBeforeTest(@NotNull CoapTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        deviceProfile = createCoapDeviceProfile(config);
        assertNotNull(deviceProfile);
        savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName());
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        assertNotNull(deviceCredentials);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    protected DeviceProfile createCoapDeviceProfile(@NotNull CoapTestConfigProperties config) throws Exception {
        CoapDeviceType coapDeviceType = config.getCoapDeviceType();
        if (coapDeviceType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            TransportPayloadType transportPayloadType = config.getTransportPayloadType();
            @NotNull DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            @NotNull DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
            @NotNull DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            deviceProfile.setTransportType(DeviceTransportType.COAP);
            @NotNull CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = new CoapDeviceProfileTransportConfiguration();
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration;
            if (CoapDeviceType.DEFAULT.equals(coapDeviceType)) {
                @NotNull DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
                if (TransportPayloadType.PROTOBUF.equals(transportPayloadType)) {
                    @NotNull ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
                    String telemetryProtoSchema = config.getTelemetryProtoSchema();
                    String attributesProtoSchema = config.getAttributesProtoSchema();
                    String rpcResponseProtoSchema = config.getRpcResponseProtoSchema();
                    String rpcRequestProtoSchema = config.getRpcRequestProtoSchema();
                    protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(
                            telemetryProtoSchema != null ? telemetryProtoSchema : DEVICE_TELEMETRY_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(
                            attributesProtoSchema != null ? attributesProtoSchema : DEVICE_ATTRIBUTES_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(
                            rpcResponseProtoSchema != null ? rpcResponseProtoSchema : DEVICE_RPC_RESPONSE_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(
                            rpcRequestProtoSchema != null ? rpcRequestProtoSchema : DEVICE_RPC_REQUEST_PROTO_SCHEMA
                    );
                    transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
                } else {
                    transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
                }
                defaultCoapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
                coapDeviceTypeConfiguration = defaultCoapDeviceTypeConfiguration;
            } else {
                coapDeviceTypeConfiguration = new EfentoCoapDeviceTypeConfiguration();
            }
            coapDeviceProfileTransportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
            deviceProfileData.setTransportConfiguration(coapDeviceProfileTransportConfiguration);
            DeviceProfileProvisionConfiguration provisionConfiguration;
            switch (provisionType) {
                case ALLOW_CREATE_NEW_DEVICES:
                    provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case CHECK_PRE_PROVISIONED_DEVICES:
                    provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case DISABLED:
                default:
                    provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
            }
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfileData.setConfiguration(configuration);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);
            return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        }
    }

    protected Device createDevice(String name, String type) throws Exception {
        @NotNull Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }
}
