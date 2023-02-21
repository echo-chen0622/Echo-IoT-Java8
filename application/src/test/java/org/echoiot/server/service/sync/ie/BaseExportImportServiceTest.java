package org.echoiot.server.service.sync.ie;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.debug.TbMsgGeneratorNode;
import org.echoiot.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.echoiot.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.data.DeviceData;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.sync.ThrowingRunnable;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityExportSettings;
import org.echoiot.server.common.data.sync.ie.EntityImportResult;
import org.echoiot.server.common.data.sync.ie.EntityImportSettings;
import org.echoiot.server.controller.AbstractControllerTest;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.echoiot.server.service.sync.vc.data.SimpleEntitiesExportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseExportImportServiceTest extends AbstractControllerTest {

    @Resource
    protected EntitiesExportImportService exportImportService;
    @Resource
    protected DeviceService deviceService;
    @Resource
    protected OtaPackageService otaPackageService;
    @Resource
    protected DeviceProfileService deviceProfileService;
    @Resource
    protected AssetProfileService assetProfileService;
    @Resource
    protected AssetService assetService;
    @Resource
    protected CustomerService customerService;
    @Resource
    protected RuleChainService ruleChainService;
    @Resource
    protected DashboardService dashboardService;
    @Resource
    protected RelationService relationService;
    @Resource
    protected TenantService tenantService;
    @Resource
    protected EntityViewService entityViewService;

    protected TenantId tenantId1;
    protected User tenantAdmin1;

    protected TenantId tenantId2;
    protected User tenantAdmin2;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
        @NotNull Tenant tenant1 = new Tenant();
        tenant1.setTitle("Tenant 1");
        tenant1.setEmail("tenant1@echoiot.org");
        this.tenantId1 = tenantService.saveTenant(tenant1).getId();
        @NotNull User tenantAdmin1 = new User();
        tenantAdmin1.setTenantId(tenantId1);
        tenantAdmin1.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin1.setEmail("tenant1-admin@echoiot.org");
        this.tenantAdmin1 = createUser(tenantAdmin1, "12345678");
        @NotNull Tenant tenant2 = new Tenant();
        tenant2.setTitle("Tenant 2");
        tenant2.setEmail("tenant2@echoiot.org");
        this.tenantId2 = tenantService.saveTenant(tenant2).getId();
        @NotNull User tenantAdmin2 = new User();
        tenantAdmin2.setTenantId(tenantId2);
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setEmail("tenant2-admin@echoiot.org");
        this.tenantAdmin2 = createUser(tenantAdmin2, "12345678");
    }

    @After
    public void afterEach() {
        tenantService.deleteTenant(tenantId1);
        tenantService.deleteTenant(tenantId2);
    }

    protected Device createDevice(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, String name) {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        @NotNull DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        return deviceService.saveDevice(device);
    }

    protected OtaPackage createOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        @NotNull OtaPackage otaPackage = new OtaPackage();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(type);
        otaPackage.setTitle("My " + type);
        otaPackage.setVersion("v1.0");
        otaPackage.setFileName("filename.txt");
        otaPackage.setContentType("text/plain");
        otaPackage.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        otaPackage.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        otaPackage.setDataSize(1L);
        otaPackage.setData(ByteBuffer.wrap(new byte[]{(int) 1}));
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    protected void checkImportedDeviceData(@NotNull Device initialDevice, @NotNull Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }

    protected DeviceProfile createDeviceProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName(name);
        deviceProfile.setDescription("dscrptn");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);
        deviceProfile.setDefaultDashboardId(defaultDashboardId);
        @NotNull DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(profileData);
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    protected void checkImportedDeviceProfileData(@NotNull DeviceProfile initialProfile, @NotNull DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected AssetProfile createAssetProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        @NotNull AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDescription("dscrptn");
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        return assetProfileService.saveAssetProfile(assetProfile);
    }

    protected void checkImportedAssetProfileData(@NotNull AssetProfile initialProfile, @NotNull AssetProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected Asset createAsset(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, String name) {
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomerId(customerId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return assetService.saveAsset(asset);
    }

    protected void checkImportedAssetData(@NotNull Asset initialAsset, @NotNull Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }

    protected Customer createCustomer(TenantId tenantId, String name) {
        @NotNull Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return customerService.saveCustomer(customer);
    }

    protected void checkImportedCustomerData(@NotNull Customer initialCustomer, @NotNull Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }

    protected Dashboard createDashboard(TenantId tenantId, @Nullable CustomerId customerId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = dashboardService.saveDashboard(dashboard);
        if (customerId != null) {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId);
            return dashboardService.findDashboardById(tenantId, dashboard.getId());
        }
        return dashboard;
    }

    protected Dashboard createDashboard(TenantId tenantId, CustomerId customerId, String name, @NotNull AssetId assetForEntityAlias) {
        Dashboard dashboard = createDashboard(tenantId, customerId, name);
        @NotNull String entityAliases = "{\n" +
                                        "\t\"23c4185d-1497-9457-30b2-6d91e69a5b2c\": {\n" +
                                        "\t\t\"alias\": \"assets\",\n" +
                                        "\t\t\"filter\": {\n" +
                                        "\t\t\t\"entityList\": [\n" +
                                        "\t\t\t\t\"" + assetForEntityAlias.getId().toString() + "\"\n" +
                                        "\t\t\t],\n" +
                                        "\t\t\t\"entityType\": \"ASSET\",\n" +
                                        "\t\t\t\"resolveMultiple\": true,\n" +
                                        "\t\t\t\"type\": \"entityList\"\n" +
                                        "\t\t},\n" +
                                        "\t\t\"id\": \"23c4185d-1497-9457-30b2-6d91e69a5b2c\"\n" +
                                        "\t}\n" +
                                        "}";
        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        return dashboardService.saveDashboard(dashboard);
    }

    protected void checkImportedDashboardData(@NotNull Dashboard initialDashboard, @NotNull Dashboard importedDashboard) {
        assertThat(importedDashboard.getTitle()).isEqualTo(initialDashboard.getTitle());
        assertThat(importedDashboard.getConfiguration()).isEqualTo(initialDashboard.getConfiguration());
        assertThat(importedDashboard.getImage()).isEqualTo(initialDashboard.getImage());
        assertThat(importedDashboard.isMobileHide()).isEqualTo(initialDashboard.isMobileHide());
        if (initialDashboard.getAssignedCustomers() != null) {
            assertThat(importedDashboard.getAssignedCustomers()).containsAll(initialDashboard.getAssignedCustomers());
        }
    }
    protected RuleChain createRuleChain(TenantId tenantId, String name, @NotNull EntityId originatorId) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        @NotNull RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        @NotNull RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Generator 1");
        ruleNode1.setType(TbMsgGeneratorNode.class.getName());
        ruleNode1.setDebugMode(true);
        @NotNull TbMsgGeneratorNodeConfiguration configuration1 = new TbMsgGeneratorNodeConfiguration();
        configuration1.setOriginatorType(originatorId.getEntityType());
        configuration1.setOriginatorId(originatorId.getId().toString());
        ruleNode1.setConfiguration(mapper.valueToTree(configuration1));

        @NotNull RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.echoiot.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setDebugMode(true);
        @NotNull TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(mapper.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, "Success");
        ruleChainService.saveRuleChainMetaData(tenantId, metaData);

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
    }

    protected RuleChain createRuleChain(TenantId tenantId, String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        @NotNull RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        @NotNull RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.echoiot.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setDebugMode(true);
        @NotNull TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(mapper.valueToTree(configuration1));

        @NotNull RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.echoiot.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setDebugMode(true);
        @NotNull TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(mapper.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, "Success");
        ruleChainService.saveRuleChainMetaData(tenantId, metaData);

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
    }

    protected void checkImportedRuleChainData(@NotNull RuleChain initialRuleChain, @NotNull RuleChainMetaData initialMetaData, @NotNull RuleChain importedRuleChain, @NotNull RuleChainMetaData importedMetaData) {
        assertThat(importedRuleChain.getType()).isEqualTo(initialRuleChain.getType());
        assertThat(importedRuleChain.getName()).isEqualTo(initialRuleChain.getName());
        assertThat(importedRuleChain.isDebugMode()).isEqualTo(initialRuleChain.isDebugMode());
        assertThat(importedRuleChain.getConfiguration()).isEqualTo(initialRuleChain.getConfiguration());

        assertThat(importedMetaData.getConnections()).isEqualTo(initialMetaData.getConnections());
        assertThat(importedMetaData.getFirstNodeIndex()).isEqualTo(initialMetaData.getFirstNodeIndex());
        for (int i = 0; i < initialMetaData.getNodes().size(); i++) {
            RuleNode initialNode = initialMetaData.getNodes().get(i);
            RuleNode importedNode = importedMetaData.getNodes().get(i);
            assertThat(importedNode.getRuleChainId()).isEqualTo(importedRuleChain.getId());
            assertThat(importedNode.getName()).isEqualTo(initialNode.getName());
            assertThat(importedNode.getType()).isEqualTo(initialNode.getType());
            assertThat(importedNode.getConfiguration()).isEqualTo(initialNode.getConfiguration());
            assertThat(importedNode.getAdditionalInfo()).isEqualTo(initialNode.getAdditionalInfo());
        }
    }

    protected EntityView createEntityView(TenantId tenantId, CustomerId customerId, EntityId entityId, String name) {
        @NotNull EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setEntityId(entityId);
        entityView.setCustomerId(customerId);
        entityView.setName(name);
        entityView.setType("A");
        return entityViewService.saveEntityView(entityView);
    }

    @NotNull
    protected EntityRelation createRelation(EntityId from, EntityId to) {
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.MANAGES_TYPE);
        relation.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relationService.saveRelation(TenantId.SYS_TENANT_ID, relation);
        return relation;
    }

    protected <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(@NotNull TenantId tenantId1, @NotNull E initialEntity, TenantId tenantId2, @NotNull E importedEntity) {
        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
        }
    }


    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(@NotNull User user, I entityId) throws Exception {
        return exportEntity(user, entityId, EntityExportSettings.builder()
                .exportCredentials(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(@NotNull User user, I entityId, EntityExportSettings exportSettings) throws Exception {
        return exportImportService.exportEntity(new SimpleEntitiesExportCtx(getSecurityUser(user), null, null, exportSettings), entityId);
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(@NotNull User user, EntityExportData<E> exportData) throws Exception {
        return importEntity(user, exportData, EntityImportSettings.builder()
                .saveCredentials(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(@NotNull User user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws Exception {
        @NotNull EntitiesImportCtx ctx = new EntitiesImportCtx(UUID.randomUUID(), getSecurityUser(user), null, importSettings);
        ctx.setFinalImportAttempt(true);
        exportData = JacksonUtil.treeToValue(JacksonUtil.valueToTree(exportData), EntityExportData.class);
        EntityImportResult<E> importResult = exportImportService.importEntity(ctx, exportData);
        exportImportService.saveReferencesAndRelations(ctx);
        for (@NotNull ThrowingRunnable throwingRunnable : ctx.getEventCallbacks()) {
            throwingRunnable.run();
        }
        return importResult;
    }

    @NotNull
    protected SecurityUser getSecurityUser(@NotNull User user) {
        return new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail()));
    }

}
