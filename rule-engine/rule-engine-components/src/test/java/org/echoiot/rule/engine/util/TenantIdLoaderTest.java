package org.echoiot.rule.engine.util;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.echoiot.rule.engine.api.*;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.rpc.Rpc;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.data.widget.WidgetType;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantIdLoaderTest {

    @Mock
    private TbContext ctx;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private RuleEngineAlarmService alarmService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private EntityViewService entityViewService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private EdgeService edgeService;
    @Mock
    private OtaPackageService otaPackageService;
    @Mock
    private RuleEngineAssetProfileCache assetProfileCache;
    @Mock
    private RuleEngineDeviceProfileCache deviceProfileCache;
    @Mock
    private WidgetTypeService widgetTypeService;
    @Mock
    private WidgetsBundleService widgetsBundleService;
    @Mock
    private QueueService queueService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private RuleEngineRpcService rpcService;
    @Mock
    private RuleEngineApiUsageStateService ruleEngineApiUsageStateService;

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;
    private AbstractListeningExecutor dbExecutor;

    @Before
    public void before() {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();
        this.tenantId = new TenantId(UUID.randomUUID());
        this.tenantProfileId = new TenantProfileId(UUID.randomUUID());

        when(ctx.getTenantId()).thenReturn(tenantId);

        for (@NotNull EntityType entityType : EntityType.values()) {
            initMocks(entityType, tenantId);
        }
    }

    @After
    public void after() {
        dbExecutor.destroy();
    }

    private void initMocks(@NotNull EntityType entityType, TenantId tenantId) {
        switch (entityType) {
            case TENANT:
                break;
            case CUSTOMER:
                @NotNull Customer customer = new Customer();
                customer.setTenantId(tenantId);

                when(ctx.getCustomerService()).thenReturn(customerService);
                doReturn(customer).when(customerService).findCustomerById(eq(tenantId), any());

                break;
            case USER:
                @NotNull User user = new User();
                user.setTenantId(tenantId);

                when(ctx.getUserService()).thenReturn(userService);
                doReturn(user).when(userService).findUserById(eq(tenantId), any());

                break;
            case ASSET:
                @NotNull Asset asset = new Asset();
                asset.setTenantId(tenantId);

                when(ctx.getAssetService()).thenReturn(assetService);
                doReturn(asset).when(assetService).findAssetById(eq(tenantId), any());

                break;
            case DEVICE:
                @NotNull Device device = new Device();
                device.setTenantId(tenantId);

                when(ctx.getDeviceService()).thenReturn(deviceService);
                doReturn(device).when(deviceService).findDeviceById(eq(tenantId), any());

                break;
            case ALARM:
                @NotNull Alarm alarm = new Alarm();
                alarm.setTenantId(tenantId);

                when(ctx.getAlarmService()).thenReturn(alarmService);
                doReturn(alarm).when(alarmService).findAlarmById(eq(tenantId), any());

                break;
            case RULE_CHAIN:
                @NotNull RuleChain ruleChain = new RuleChain();
                ruleChain.setTenantId(tenantId);

                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleChain).when(ruleChainService).findRuleChainById(eq(tenantId), any());

                break;
            case ENTITY_VIEW:
                @NotNull EntityView entityView = new EntityView();
                entityView.setTenantId(tenantId);

                when(ctx.getEntityViewService()).thenReturn(entityViewService);
                doReturn(entityView).when(entityViewService).findEntityViewById(eq(tenantId), any());

                break;
            case DASHBOARD:
                @NotNull Dashboard dashboard = new Dashboard();
                dashboard.setTenantId(tenantId);

                when(ctx.getDashboardService()).thenReturn(dashboardService);
                doReturn(dashboard).when(dashboardService).findDashboardById(eq(tenantId), any());

                break;
            case EDGE:
                @NotNull Edge edge = new Edge();
                edge.setTenantId(tenantId);

                when(ctx.getEdgeService()).thenReturn(edgeService);
                doReturn(edge).when(edgeService).findEdgeById(eq(tenantId), any());

                break;
            case OTA_PACKAGE:
                @NotNull OtaPackage otaPackage = new OtaPackage();
                otaPackage.setTenantId(tenantId);

                when(ctx.getOtaPackageService()).thenReturn(otaPackageService);
                doReturn(otaPackage).when(otaPackageService).findOtaPackageInfoById(eq(tenantId), any());

                break;
            case ASSET_PROFILE:
                @NotNull AssetProfile assetProfile = new AssetProfile();
                assetProfile.setTenantId(tenantId);

                when(ctx.getAssetProfileCache()).thenReturn(assetProfileCache);
                doReturn(assetProfile).when(assetProfileCache).get(eq(tenantId), any(AssetProfileId.class));

                break;
            case DEVICE_PROFILE:
                @NotNull DeviceProfile deviceProfile = new DeviceProfile();
                deviceProfile.setTenantId(tenantId);

                when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);
                doReturn(deviceProfile).when(deviceProfileCache).get(eq(tenantId), any(DeviceProfileId.class));

                break;
            case WIDGET_TYPE:
                @NotNull WidgetType widgetType = new WidgetType();
                widgetType.setTenantId(tenantId);

                when(ctx.getWidgetTypeService()).thenReturn(widgetTypeService);
                doReturn(widgetType).when(widgetTypeService).findWidgetTypeById(eq(tenantId), any());

                break;
            case WIDGETS_BUNDLE:
                @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
                widgetsBundle.setTenantId(tenantId);

                when(ctx.getWidgetBundleService()).thenReturn(widgetsBundleService);
                doReturn(widgetsBundle).when(widgetsBundleService).findWidgetsBundleById(eq(tenantId), any());

                break;
            case RPC:
                @NotNull Rpc rpc = new Rpc();
                rpc.setTenantId(tenantId);

                when(ctx.getRpcService()).thenReturn(rpcService);
                doReturn(rpc).when(rpcService).findRpcById(eq(tenantId), any());

                break;
            case QUEUE:
                @NotNull Queue queue = new Queue();
                queue.setTenantId(tenantId);

                when(ctx.getQueueService()).thenReturn(queueService);
                doReturn(queue).when(queueService).findQueueById(eq(tenantId), any());

                break;
            case API_USAGE_STATE:
                @NotNull ApiUsageState apiUsageState = new ApiUsageState();
                apiUsageState.setTenantId(tenantId);

                when(ctx.getRuleEngineApiUsageStateService()).thenReturn(ruleEngineApiUsageStateService);
                doReturn(apiUsageState).when(ruleEngineApiUsageStateService).findApiUsageStateById(eq(tenantId), any());

                break;
            case TB_RESOURCE:
                @NotNull TbResource tbResource = new TbResource();
                tbResource.setTenantId(tenantId);

                when(ctx.getResourceService()).thenReturn(resourceService);
                doReturn(tbResource).when(resourceService).findResourceInfoById(eq(tenantId), any());

                break;
            case RULE_NODE:
                @NotNull RuleNode ruleNode = new RuleNode();

                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleNode).when(ruleChainService).findRuleNodeById(eq(tenantId), any());

                break;
            case TENANT_PROFILE:
                @NotNull TenantProfile tenantProfile = new TenantProfile(tenantProfileId);

                when(ctx.getTenantProfile()).thenReturn(tenantProfile);

                break;
            default:
                throw new RuntimeException("Unexpected original EntityType " + entityType);
        }

    }

    private EntityId getEntityId(@NotNull EntityType entityType) {
        return EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());
    }

    private void checkTenant(TenantId checkTenantId, boolean equals) {
        for (@NotNull EntityType entityType : EntityType.values()) {
            EntityId entityId;
            if (EntityType.TENANT.equals(entityType)) {
                entityId = tenantId;
            } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                entityId = tenantProfileId;
            } else {
                entityId = getEntityId(entityType);
            }
            @Nullable TenantId targetTenantId = TenantIdLoader.findTenantId(ctx, entityId);
            @NotNull String msg = "Check entity type <" + entityType.name() + ">:";
            if (equals) {
                Assert.assertEquals(msg, targetTenantId, checkTenantId);
            } else {
                Assert.assertNotEquals(msg, targetTenantId, checkTenantId);
            }
        }
    }

    @Test
    public void test_findEntityIdAsync_current_tenant() {
        checkTenant(tenantId, true);
    }

    @Test
    public void test_findEntityIdAsync_other_tenant() {
        checkTenant(new TenantId(UUID.randomUUID()), false);
    }

}
