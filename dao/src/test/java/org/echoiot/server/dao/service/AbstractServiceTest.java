package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.event.RuleNodeDebugEvent;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.HasId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.audit.AuditLogLevelFilter;
import org.echoiot.server.dao.audit.AuditLogLevelMask;
import org.echoiot.server.dao.audit.AuditLogLevelProperties;
import org.echoiot.server.dao.component.ComponentDescriptorService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.event.EventService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rpc.RpcService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractServiceTest.class, loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan("org.echoiot.server")
public abstract class AbstractServiceTest {

    @NotNull
    protected ObjectMapper mapper = new ObjectMapper();

    public static final TenantId SYSTEM_TENANT_ID = TenantId.SYS_TENANT_ID;

    @Resource
    protected UserService userService;

    @Resource
    protected ApiUsageStateService apiUsageStateService;

    @Resource
    protected AdminSettingsService adminSettingsService;

    @Resource
    protected TenantService tenantService;

    @Resource
    protected CustomerService customerService;

    @Resource
    protected DeviceService deviceService;

    @Resource
    protected AssetService assetService;

    @Resource
    protected EntityViewService entityViewService;

    @Resource
    protected EntityService entityService;

    @Resource
    protected DeviceCredentialsService deviceCredentialsService;

    @Resource
    protected WidgetsBundleService widgetsBundleService;

    @Resource
    protected WidgetTypeService widgetTypeService;

    @Resource
    protected DashboardService dashboardService;

    @Resource
    protected TimeseriesService tsService;

    @Resource
    protected EventService eventService;

    @Resource
    protected RelationService relationService;

    @Resource
    protected AlarmService alarmService;

    @Resource
    protected RuleChainService ruleChainService;

    @Resource
    protected EdgeService edgeService;

    @Resource
    protected EdgeEventService edgeEventService;

    @Resource
    private ComponentDescriptorService componentDescriptorService;

    @Resource
    protected TenantProfileService tenantProfileService;

    @Resource
    protected DeviceProfileService deviceProfileService;

    @Resource
    protected AssetProfileService assetProfileService;

    @Resource
    protected ResourceService resourceService;

    @Resource
    protected OtaPackageService otaPackageService;

    @Resource
    protected RpcService rpcService;

    @Resource
    protected QueueService queueService;

    public class IdComparator<D extends HasId> implements Comparator<D> {
        @Override
        public int compare(@NotNull D o1, @NotNull D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }


    protected RuleNodeDebugEvent generateEvent(@Nullable TenantId tenantId, @NotNull EntityId entityId) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        return RuleNodeDebugEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId("server A")
                .data(JacksonUtil.toString(readFromResource("TestJsonData.json")))
                .build();
    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource) throws IOException {
//        return getOrCreateDescriptor(scope, type, clazz, configurationDescriptorResource, null);
//    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource, String actions) throws IOException {
//        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(clazz);
//        if (descriptor == null) {
//            descriptor = new ComponentDescriptor();
//            descriptor.setName("test");
//            descriptor.setClazz(clazz);
//            descriptor.setScope(scope);
//            descriptor.setType(type);
//            descriptor.setActions(actions);
//            descriptor.setConfigurationDescriptor(readFromResource(configurationDescriptorResource));
//            componentDescriptorService.saveComponent(descriptor);
//        }
//        return descriptor;
//    }

    public JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @NotNull
    @Bean
    public AuditLogLevelFilter auditLogLevelFilter() {
        @NotNull Map<String, String> mask = new HashMap<>();
        for (@NotNull EntityType entityType : EntityType.values()) {
            mask.put(entityType.name().toLowerCase(), AuditLogLevelMask.RW.name());
        }
        @NotNull var props = new AuditLogLevelProperties();
        props.setMask(mask);
        return new AuditLogLevelFilter(props);
    }

    @NotNull
    protected DeviceProfile createDeviceProfile(TenantId tenantId, String name) {
        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName(name);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription(name + " Test");
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        @NotNull DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        @NotNull DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    @NotNull
    protected AssetProfile createAssetProfile(TenantId tenantId, String name) {
        @NotNull AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDescription(name + " Test");
        assetProfile.setDefault(false);
        assetProfile.setDefaultRuleChainId(null);
        return assetProfile;
    }

    public TenantId createTenant() {
        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant " + Uuids.timeBased());
        Tenant savedTenant = tenantService.saveTenant(tenant);
        assertNotNull(savedTenant);
        return savedTenant.getId();
    }

    @NotNull
    protected Edge constructEdge(TenantId tenantId, String name, String type) {
        @NotNull Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType(type);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        return edge;
    }

    @NotNull
    protected OtaPackage constructDefaultOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId) {
        @NotNull OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(OtaPackageType.FIRMWARE);
        firmware.setTitle("My firmware");
        firmware.setVersion("3.3.3");
        firmware.setFileName("filename.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        return firmware;
    }

}
