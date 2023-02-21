package org.echoiot.server.edge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.device.data.DefaultDeviceConfiguration;
import org.echoiot.server.common.data.device.data.DeviceData;
import org.echoiot.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.query.EntityKeyValueType;
import org.echoiot.server.common.data.query.FilterPredicateValue;
import org.echoiot.server.common.data.query.NumericFilterPredicate;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.controller.AbstractControllerTest;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.edge.imitator.EdgeImitator;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.echoiot.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
abstract public class AbstractEdgeTest extends AbstractControllerTest {

    private static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    protected Tenant savedTenant;
    protected TenantId tenantId;
    protected User tenantAdmin;

    protected DeviceProfile thermostatDeviceProfile;

    protected EdgeImitator edgeImitator;
    protected Edge edge;

    @Resource
    protected EdgeEventService edgeEventService;

    @Resource
    protected DataDecodingEncodingService dataDecodingEncodingService;

    @Resource
    protected TbClusterService clusterService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
        // sleep 0.5 second to avoid CREDENTIALS updated message for the user
        // user credentials is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event edge could be already added and additional message will be pushed
        Thread.sleep(500);

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.expectMessageAmount(21);
        edgeImitator.connect();

        requestEdgeRuleChainMetadata();

        verifyEdgeConnectionAndInitialData();
    }

    private void requestEdgeRuleChainMetadata() throws Exception {
        RuleChainId rootRuleChainId = getEdgeRootRuleChainId();
        @NotNull RuleChainMetadataRequestMsg.Builder builder = RuleChainMetadataRequestMsg.newBuilder()
                                                                                          .setRuleChainIdMSB(rootRuleChainId.getId().getMostSignificantBits())
                                                                                          .setRuleChainIdLSB(rootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(builder);
        @NotNull UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                                                               .addRuleChainMetadataRequestMsg(builder.build());
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
    }

    private RuleChainId getEdgeRootRuleChainId() throws Exception {
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        for (@NotNull RuleChain edgeRuleChain : edgeRuleChains) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain.getId();
            }
        }
        throw new RuntimeException("Root rule chain not found");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getUuidId())
                .andExpect(status().isOk());

        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {}
    }

    private void installation() {
        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME,
                createMqttDeviceProfileTransportConfiguration(new JsonTransportPayloadConfiguration(), false));

        extendDeviceProfileData(thermostatDeviceProfile);
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", THERMOSTAT_DEVICE_PROFILE_NAME);
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);

        Asset savedAsset = saveAsset("Edge Asset 1");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
    }

    protected void extendDeviceProfileData(@NotNull DeviceProfile deviceProfile) {
        @Nullable DeviceProfileData profileData = deviceProfile.getProfileData();
        @NotNull List<DeviceProfileAlarm> alarms = new ArrayList<>();
        @NotNull DeviceProfileAlarm deviceProfileAlarm = new DeviceProfileAlarm();
        deviceProfileAlarm.setAlarmType("High Temperature");
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Alarm Details");
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        @NotNull List<AlarmConditionFilter> condition = new ArrayList<>();
        @NotNull AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperature"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(55.0));
        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.NUMERIC);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        deviceProfileAlarm.setClearRule(alarmRule);
        @NotNull TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        deviceProfileAlarm.setCreateRules(createRules);
        alarms.add(deviceProfileAlarm);
        profileData.setAlarms(alarms);
        profileData.setProvisionConfiguration(new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("123"));
    }

    private void verifyEdgeConnectionAndInitialData() throws Exception {
        Assert.assertTrue(edgeImitator.waitForMessages());

        validateEdgeConfiguration();

        // 5 messages
        // - 2 from device profile fetcher (default and thermostat)
        // - 1 from device fetcher
        // - 1 from device profile controller (thermostat)
        // - 1 from device controller (thermostat)
        validateDeviceProfiles();

        // 2 messages - 1 from device fetcher and 1 from device controller
        validateDevices();

        // 2 messages - 1 from asset fetcher and 1 from asset controller
        validateAssets();

        // 2 messages - 1 from rule chain fetcher and 1 from rule chain controller
        @NotNull UUID ruleChainUUID = validateRuleChains();

        // 1 from request message
        validateRuleChainMetadataUpdates(ruleChainUUID);

        // 4 messages - 4 messages from fetcher - 2 from system level ('mail', 'mailTemplates') and 2 from admin level ('mail', 'mailTemplates')
        validateAdminSettings();

        // 3 messages
        // - 1 message from asset profile fetcher
        // - 1 message from asset fetcher
        // - 1 message from asset controller
        validateAssetProfiles();

        // 1 message from queue fetcher
        validateQueues();

        // 1 message from user fetcher
        validateUsers();
    }

    private void validateEdgeConfiguration() throws Exception {
        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);
        testAutoGeneratedCodeByProtobuf(configuration);
    }

    private void validateDeviceProfiles() throws Exception {
        @NotNull List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        // default msg
        // thermostat msg from fetcher
        // thermostat msg from device fetcher
        // thermostat msg from controller
        // thermostat msg from creation of device
        Assert.assertEquals(5, deviceProfileUpdateMsgList.size());
        @NotNull Optional<DeviceProfileUpdateMsg> thermostatProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> THERMOSTAT_DEVICE_PROFILE_NAME.equals(dfum.getName())).findAny();
        Assert.assertTrue(thermostatProfileUpdateMsgOpt.isPresent());
        @NotNull DeviceProfileUpdateMsg thermostatProfileUpdateMsg = thermostatProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, thermostatProfileUpdateMsg.getMsgType());
        @NotNull UUID deviceProfileUUID = new UUID(thermostatProfileUpdateMsg.getIdMSB(), thermostatProfileUpdateMsg.getIdLSB());
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotNull(deviceProfile.getProfileData());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms().get(0).getClearRule());

        testAutoGeneratedCodeByProtobuf(thermostatProfileUpdateMsg);
    }

    private void validateDevices() throws Exception {
        @NotNull List<DeviceUpdateMsg> deviceUpdateMsgs = edgeImitator.findAllMessagesByType(DeviceUpdateMsg.class);
        Assert.assertEquals(2, deviceUpdateMsgs.size());
        validateDevice(deviceUpdateMsgs.get(0));
        validateDevice(deviceUpdateMsgs.get(1));
    }

    private void validateDevice(@NotNull DeviceUpdateMsg deviceUpdateMsg) throws Exception {
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        @NotNull UUID deviceUUID = new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB());
        Device device = doGet("/api/device/" + deviceUUID, Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeDevices.contains(device));

        testAutoGeneratedCodeByProtobuf(deviceUpdateMsg);
    }

    private void validateAssets() throws Exception {
        @NotNull List<AssetUpdateMsg> assetUpdateMsgs = edgeImitator.findAllMessagesByType(AssetUpdateMsg.class);
        Assert.assertEquals(2, assetUpdateMsgs.size());
        validateAsset(assetUpdateMsgs.get(0));
        validateAsset(assetUpdateMsgs.get(1));
    }

    private void validateAsset(@NotNull AssetUpdateMsg assetUpdateMsg) throws Exception {
        Assert.assertNotNull(assetUpdateMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        @NotNull UUID assetUUID = new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB());
        Asset asset = doGet("/api/asset/" + assetUUID, Asset.class);
        Assert.assertNotNull(asset);
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeAssets.contains(asset));

        testAutoGeneratedCodeByProtobuf(assetUpdateMsg);
    }

    @NotNull
    private UUID validateRuleChains() throws Exception {
        @NotNull List<RuleChainUpdateMsg> ruleChainUpdateMsgs = edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class);
        Assert.assertEquals(2, ruleChainUpdateMsgs.size());
        RuleChainUpdateMsg ruleChainCreateMsg = ruleChainUpdateMsgs.get(0);
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgs.get(1);
        validateRuleChain(ruleChainCreateMsg, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        validateRuleChain(ruleChainUpdateMsg, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        return new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
    }

    private void validateRuleChain(@NotNull RuleChainUpdateMsg ruleChainUpdateMsg, UpdateMsgType expectedMsgType) throws Exception {
        Assert.assertEquals(expectedMsgType, ruleChainUpdateMsg.getMsgType());
        @NotNull UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));
        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);
    }

    private void validateRuleChainMetadataUpdates(UUID expectedRuleChainUUID) {
        @NotNull Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateOpt.isPresent());
        @NotNull RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainMetadataUpdateMsg.getMsgType());
        @NotNull UUID ruleChainUUID = new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB());
        Assert.assertEquals(expectedRuleChainUUID, ruleChainUUID);
    }

    private void validateAdminSettings() throws JsonProcessingException {
        @NotNull List<AdminSettingsUpdateMsg> adminSettingsUpdateMsgs = edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class);
        Assert.assertEquals(4, adminSettingsUpdateMsgs.size());

        for (@NotNull AdminSettingsUpdateMsg adminSettingsUpdateMsg : adminSettingsUpdateMsgs) {
            if (adminSettingsUpdateMsg.getKey().equals("mail")) {
                validateMailAdminSettings(adminSettingsUpdateMsg);
            }
            if (adminSettingsUpdateMsg.getKey().equals("mailTemplates")) {
                validateMailTemplatesAdminSettings(adminSettingsUpdateMsg);
            }
        }
    }

    private void validateMailAdminSettings(@NotNull AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("mailFrom"));
        Assert.assertNotNull(jsonNode.get("smtpProtocol"));
        Assert.assertNotNull(jsonNode.get("smtpHost"));
        Assert.assertNotNull(jsonNode.get("smtpPort"));
        Assert.assertNotNull(jsonNode.get("timeout"));
    }

    private void validateMailTemplatesAdminSettings(@NotNull AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("accountActivated"));
        Assert.assertNotNull(jsonNode.get("accountLockout"));
        Assert.assertNotNull(jsonNode.get("activation"));
        Assert.assertNotNull(jsonNode.get("passwordWasReset"));
        Assert.assertNotNull(jsonNode.get("resetPassword"));
        Assert.assertNotNull(jsonNode.get("test"));
    }

    private void validateAssetProfiles() throws Exception {
        @NotNull List<AssetProfileUpdateMsg> assetProfileUpdateMsgs = edgeImitator.findAllMessagesByType(AssetProfileUpdateMsg.class);
        Assert.assertEquals(3, assetProfileUpdateMsgs.size());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        @NotNull UUID assetProfileUUID = new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB());
        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileUUID, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("default", assetProfile.getName());
        Assert.assertTrue(assetProfile.isDefault());
        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsg);
    }

    private void validateQueues() throws Exception {
        @NotNull Optional<QueueUpdateMsg> queueUpdateMsgOpt = edgeImitator.findMessageByType(QueueUpdateMsg.class);
        Assert.assertTrue(queueUpdateMsgOpt.isPresent());
        @NotNull QueueUpdateMsg queueUpdateMsg = queueUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        @NotNull UUID queueUUID = new UUID(queueUpdateMsg.getIdMSB(), queueUpdateMsg.getIdLSB());
        Queue queue = doGet("/api/queues/" + queueUUID, Queue.class);
        Assert.assertNotNull(queue);
        Assert.assertEquals(DataConstants.MAIN_QUEUE_NAME, queueUpdateMsg.getName());
        Assert.assertEquals(DataConstants.MAIN_QUEUE_TOPIC, queueUpdateMsg.getTopic());
        Assert.assertEquals(10, queueUpdateMsg.getPartitions());
        Assert.assertEquals(25, queueUpdateMsg.getPollInterval());
        testAutoGeneratedCodeByProtobuf(queueUpdateMsg);
    }

    private void validateUsers() throws Exception {
        @NotNull Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        @NotNull UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        @NotNull UUID userUUID = new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB());
        User user = doGet("/api/user/" + userUUID, User.class);
        Assert.assertNotNull(user);
        Assert.assertEquals("tenant2@echoiot.org", userUpdateMsg.getEmail());
        testAutoGeneratedCodeByProtobuf(userUpdateMsg);
    }

    @NotNull
    protected Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create ota package
        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(thermostatDeviceProfile.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // create device and assign to edge
        Device savedDevice = saveDevice(StringUtils.randomAlphanumeric(15), thermostatDeviceProfile.getName());
        edgeImitator.expectMessageAmount(2); // device and device profile messages
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        @NotNull Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        @NotNull DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        @NotNull Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        @NotNull DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        // update device
        edgeImitator.expectMessageAmount(1);
        savedDevice.setFirmwareId(firmwareOtaPackageInfo.getId());

        @NotNull DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        @NotNull MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        deviceData.setTransportConfiguration(transportConfiguration);
        savedDevice.setDeviceData(deviceData);

        savedDevice = doPost("/api/device", savedDevice, Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getFirmwareIdMSB());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getFirmwareIdLSB());
        Optional<DeviceData> deviceDataOpt =
                dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
        Assert.assertTrue(deviceDataOpt.isPresent());
        deviceData = deviceDataOpt.get();
        Assert.assertTrue(deviceData.getTransportConfiguration() instanceof MqttDeviceTransportConfiguration);
        @NotNull MqttDeviceTransportConfiguration mqttDeviceTransportConfiguration =
                (MqttDeviceTransportConfiguration) deviceData.getTransportConfiguration();
        Assert.assertEquals("tb_rule_engine.thermostat", mqttDeviceTransportConfiguration.getProperties().get("topic"));
        return savedDevice;
    }

    @NotNull
    protected Device findDeviceByName(String deviceName) throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        @NotNull Optional<Device> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        @NotNull Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    @NotNull
    protected Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();

        Assert.assertEquals(1, edgeAssets.size());
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals(assetName, asset.getName());
        return asset;
    }

    protected Device saveDevice(String deviceName, String type) {
        @NotNull Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }

    protected Asset saveAsset(String assetName) {
        @NotNull Asset asset = new Asset();
        asset.setName(assetName);
        return doPost("/api/asset", asset, Asset.class);
    }

    protected OtaPackageInfo saveOtaPackageInfo(DeviceProfileId deviceProfileId) {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("Firmware Edge " + StringUtils.randomAlphanumeric(3));
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareInfo.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    @NotNull
    protected EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction,
                                           UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        @NotNull EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    protected void testAutoGeneratedCodeByProtobuf(@NotNull MessageLite.Builder builder) throws InvalidProtocolBufferException {
        MessageLite source = builder.build();

        testAutoGeneratedCodeByProtobuf(source);

        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        builder.clear().mergeFrom(target);
    }

    protected void testAutoGeneratedCodeByProtobuf(@NotNull MessageLite source) throws InvalidProtocolBufferException {
        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        Assert.assertEquals(source, target);
        Assert.assertEquals(source.hashCode(), target.hashCode());
    }



}
