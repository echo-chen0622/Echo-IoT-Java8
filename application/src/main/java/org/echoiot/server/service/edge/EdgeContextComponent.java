package org.echoiot.server.service.edge;

import freemarker.template.Configuration;
import lombok.Data;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.edge.rpc.EdgeEventStorageSettings;
import org.echoiot.server.service.edge.rpc.constructor.EdgeMsgConstructor;
import org.echoiot.server.service.edge.rpc.processor.*;
import org.echoiot.server.service.edge.rpc.sync.EdgeRequestsService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.executors.GrpcCallbackExecutorService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@TbCoreComponent
@Data
@Lazy
public class EdgeContextComponent {

    @Resource
    private TbClusterService clusterService;

    @Resource
    private EdgeService edgeService;

    @Resource
    private EdgeEventService edgeEventService;

    @Resource
    private AdminSettingsService adminSettingsService;

    @Resource
    private Configuration freemarkerConfig;

    @Resource
    private DeviceService deviceService;

    @Resource
    private AssetService assetService;

    @Resource
    private EntityViewService entityViewService;

    @Resource
    private DeviceProfileService deviceProfileService;

    @Resource
    private AssetProfileService assetProfileService;

    @Resource
    private AttributesService attributesService;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private UserService userService;

    @Resource
    private CustomerService customerService;

    @Resource
    private WidgetsBundleService widgetsBundleService;

    @Resource
    private EdgeRequestsService edgeRequestsService;

    @Resource
    private OtaPackageService otaPackageService;

    @Resource
    private QueueService queueService;

    @Resource
    private AlarmEdgeProcessor alarmProcessor;

    @Resource
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Resource
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Resource
    private EdgeProcessor edgeProcessor;

    @Resource
    private DeviceEdgeProcessor deviceProcessor;

    @Resource
    private AssetEdgeProcessor assetProcessor;

    @Resource
    private EntityViewEdgeProcessor entityViewProcessor;

    @Resource
    private UserEdgeProcessor userProcessor;

    @Resource
    private RelationEdgeProcessor relationProcessor;

    @Resource
    private TelemetryEdgeProcessor telemetryProcessor;

    @Resource
    private DashboardEdgeProcessor dashboardProcessor;

    @Resource
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Resource
    private CustomerEdgeProcessor customerProcessor;

    @Resource
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Resource
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Resource
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Resource
    private OtaPackageEdgeProcessor otaPackageEdgeProcessor;

    @Resource
    private QueueEdgeProcessor queueEdgeProcessor;

    @Resource
    private EdgeMsgConstructor edgeMsgConstructor;

    @Resource
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Resource
    private DbCallbackExecutorService dbCallbackExecutor;

    @Resource
    private GrpcCallbackExecutorService grpcCallbackExecutorService;
}
