package org.echoiot.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.credentials.lwm2m.*;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.echoiot.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.echoiot.server.transport.AbstractTransportIntegrationTest;
import org.echoiot.server.transport.lwm2m.client.LwM2MTestClient;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.echoiot.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.ResponseCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.SocketUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.*;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.eclipse.californium.core.config.CoapConfig.COAP_PORT;
import static org.eclipse.californium.core.config.CoapConfig.COAP_SECURE_PORT;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "transport.lwm2m.enabled=true",
})
@Slf4j
@DaoSqlTest
public abstract class AbstractLwM2MIntegrationTest extends AbstractTransportIntegrationTest {

    @SpyBean
    DefaultLwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandlerTest;

    @Resource
    private LwM2mClientContext clientContextTest;

    //  Lwm2m Server
    public static final int port = 5685;
    public static final int securityPort = 5686;
    public static final int portBs = 5687;
    public static final int securityPortBs = 5688;
    public static final int[] SERVERS_PORT_NUMBERS = {port, securityPort, portBs, securityPortBs};

    public static final String host = "localhost";
    public static final String hostBs = "localhost";
    public static final int shortServerId = 123;
    public static final int shortServerIdBs = 111;
    public static final String COAP = "coap://";
    public static final String COAPS = "coaps://";
    public static final String URI = COAP + host + ":" + port;
    public static final String SECURE_URI = COAPS + host + ":" + securityPort;
    public static final String URI_BS = COAP + hostBs + ":" + portBs;
    public static final String SECURE_URI_BS = COAPS + hostBs + ":" + securityPortBs;
    public static final Configuration COAP_CONFIG = new Configuration().set(COAP_PORT, port).set(COAP_SECURE_PORT, securityPort);
    @NotNull
    public static Configuration COAP_CONFIG_BS = new Configuration().set(COAP_PORT, portBs).set(COAP_SECURE_PORT, securityPortBs);
    public static final Security SECURITY_NO_SEC = noSec(URI, shortServerId);
    public static final Security SECURITY_NO_SEC_BS = noSecBootstap(URI_BS);

    protected final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String OBSERVE_ATTRIBUTES_WITH_PARAMS =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.0/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.0/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesBsSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationBsSuccess = new HashSet<>(Arrays.asList(ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected LwM2MTestClient lwM2MTestClient;
    private String[] resources;

    @Before
    public void startInit() throws Exception {
        init();
    }

    @After
    public void after() {
        clientDestroy();
        executor.shutdownNow();
    }

    @AfterClass
    public static void afterClass() {
        awaitServersDestroy();
    }

    private void init() throws Exception {
        executor = Executors.newScheduledThreadPool(10, EchoiotThreadFactory.forName("test-lwm2m-scheduled"));
        loginTenantAdmin();
        for (String resourceName : this.resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(Base64.getEncoder().encodeToString(bytes));
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
    }

    public void basicTestConnectionObserveTelemetry(@NotNull Security security,
                                                    LwM2MDeviceCredentials deviceCredentials,
                                                    @NotNull Configuration coapConfig,
                                                    String endpoint) throws Exception {
        @NotNull Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        @NotNull Device device = createDevice(deviceCredentials, endpoint);

        @NotNull SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        @NotNull LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        @NotNull EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                                                           Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        @NotNull EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        @NotNull TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        getWsClient().send(mapper.writeValueAsString(wrapper));
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();
        createNewClient(security, coapConfig, false, endpoint, false, null);
        awaitObserveReadAll(0, false, device.getId().getId().toString());
        String msg = getWsClient().waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertThat(Long.parseLong(tsValue.getValue()), instanceOf(Long.class));
        int expectedMax = 50;
        int expectedMin = 5;
        Assert.assertTrue(expectedMax >= Long.parseLong(tsValue.getValue()));
        Assert.assertTrue(expectedMin <= Long.parseLong(tsValue.getValue()));
    }

    protected void createDeviceProfile(Lwm2mDeviceProfileTransportConfiguration transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();
        deviceProfile.setName("LwM2M");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    @NotNull
    protected Device createDevice(LwM2MDeviceCredentials credentials, String endpoint) throws Exception {
        Device device = new Device();
        device.setName(endpoint);
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    @NotNull
    public NoSecClientCredential createNoSecClientCredentials(String endpoint) {
        @NotNull NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(endpoint);
        return clientCredentials;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }

    public void createNewClient(@NotNull Security security, @NotNull Configuration coapConfig, boolean isRpc, String endpoint, boolean isBootstrap, Security securityBs) throws Exception {
        this.clientDestroy();
        lwM2MTestClient = new LwM2MTestClient(this.executor, endpoint);
        int clientPort = SocketUtils.findAvailableUdpPort();
        lwM2MTestClient.init(security, coapConfig, clientPort, isRpc, isBootstrap, shortServerId, shortServerIdBs,
                securityBs, this.defaultLwM2mUplinkMsgHandlerTest, this.clientContextTest);
    }

    private void clientDestroy() {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
                awaitClientDestroy(lwM2MTestClient.getLeshanClient());
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    @NotNull
    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        @NotNull Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        @Nullable TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        @Nullable OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    @NotNull
    protected List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(@NotNull LwM2MProfileBootstrapConfigType bootstrapConfigType) {
        @NotNull List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        switch (bootstrapConfigType) {
            case BOTH:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case BOOTSTRAP_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case LWM2M_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                break;
            case NONE:
        }
        return bootstrap;
    }

    @NotNull
    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap) {
        @NotNull AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
    }

    @NotNull
    protected LwM2MDeviceCredentials getDeviceCredentialsNoSec(LwM2MClientCredential clientCredentials) {
        @NotNull LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        @NotNull LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        @NotNull NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    private static void awaitServersDestroy() {
        await("One of servers ports number is not free")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> isServerPortsAvailable() == null);
    }

    @Nullable
    private static String isServerPortsAvailable() {
        for (int port : SERVERS_PORT_NUMBERS) {
            try (@NotNull ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.close();
                Assert.assertTrue(serverSocket.isClosed());
            } catch (IOException e) {
                log.warn(String.format("Port %n still in use", port));
                return (String.format("Port %n still in use", port));
            }
        }
        return null;
    }

    private static void awaitClientDestroy(@NotNull LeshanClient leshanClient) {
        await("Destroy LeshanClient: delete All is registered Servers.")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> leshanClient.getRegisteredServers().size() == 0);
    }

    protected  void awaitObserveReadAll(int cntObserve, boolean isBootstrap, String deviceIdStr) throws Exception {
        if (!isBootstrap) {
            await("ObserveReadAll after start client: countObserve " + cntObserve)
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        String actualResultReadAll = sendObserve("ObserveReadAll", null, deviceIdStr);
                        @Nullable ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
                        Assert.assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
                        String actualValuesReadAll = rpcActualResultReadAll.get("value").asText();
                        log.warn("ObserveReadAll:  [{}]", actualValuesReadAll);
                        int actualCntObserve = "[]".equals(actualValuesReadAll) ? 0 : actualValuesReadAll.split(",").length;
                        return cntObserve == actualCntObserve;
                    });
        }
    }

    protected String sendObserve(String method, @Nullable String params, String deviceIdStr) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        }
        else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceIdStr, sendRpcRequest, String.class, status().isOk());
    }

}
