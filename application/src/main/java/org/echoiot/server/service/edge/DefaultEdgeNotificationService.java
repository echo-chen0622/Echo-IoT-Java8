package org.echoiot.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.edge.rpc.processor.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    @Resource
    private EdgeService edgeService;

    @Resource
    private EdgeEventService edgeEventService;

    @Resource
    private TbClusterService clusterService;

    @Resource
    private EdgeProcessor edgeProcessor;

    @Resource
    private AssetEdgeProcessor assetProcessor;

    @Resource
    private DeviceEdgeProcessor deviceProcessor;

    @Resource
    private EntityViewEdgeProcessor entityViewProcessor;

    @Resource
    private DashboardEdgeProcessor dashboardProcessor;

    @Resource
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Resource
    private UserEdgeProcessor userProcessor;

    @Resource
    private CustomerEdgeProcessor customerProcessor;

    @Resource
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Resource
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Resource
    private OtaPackageEdgeProcessor otaPackageProcessor;

    @Resource
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Resource
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Resource
    private QueueEdgeProcessor queueProcessor;

    @Resource
    private AlarmEdgeProcessor alarmProcessor;

    @Resource
    private RelationEdgeProcessor relationProcessor;

    private ExecutorService dbCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        dbCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("edge-notifications"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (dbCallBackExecutor != null) {
            dbCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, @NotNull Edge edge, RuleChainId ruleChainId) throws Exception {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.UPDATED, ruleChainId, isRootBody).get();
        return savedEdge;
    }

    @NotNull
    private ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                 EdgeId edgeId,
                                                 EdgeEventType type,
                                                 EdgeEventActionType action,
                                                 EntityId entityId,
                                                 JsonNode body) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        @NotNull EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            clusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallBackExecutor);
    }

    @Override
    public void pushNotificationToEdge(@NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, @NotNull TbCallback callback) {
        log.debug("Pushing notification to edge {}", edgeNotificationMsg);
        try {
            @NotNull TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            @NotNull EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            ListenableFuture<Void> future;
            switch (type) {
                case EDGE:
                    future = edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET:
                    future = assetProcessor.processAssetNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE:
                    future = deviceProcessor.processDeviceNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_VIEW:
                    future = entityViewProcessor.processEntityViewNotification(tenantId, edgeNotificationMsg);
                    break;
                case DASHBOARD:
                    future = dashboardProcessor.processDashboardNotification(tenantId, edgeNotificationMsg);
                    break;
                case RULE_CHAIN:
                    future = ruleChainProcessor.processRuleChainNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                    future = userProcessor.processUserNotification(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    future = customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE_PROFILE:
                    future = deviceProfileProcessor.processDeviceProfileNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET_PROFILE:
                    future = assetProfileProcessor.processAssetProfileNotification(tenantId, edgeNotificationMsg);
                    break;
                case OTA_PACKAGE:
                    future = otaPackageProcessor.processOtaPackageNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                    future = widgetBundleProcessor.processWidgetsBundleNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGET_TYPE:
                    future = widgetTypeProcessor.processWidgetTypeNotification(tenantId, edgeNotificationMsg);
                    break;
                case QUEUE:
                    future = queueProcessor.processQueueNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    future = alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    future = relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.warn("Edge event type [{}] is not designed to be pushed to edge", type);
                    future = Futures.immediateFuture(null);
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callBackFailure(edgeNotificationMsg, callback, throwable);
                }
            }, dbCallBackExecutor);
        } catch (Exception e) {
            callBackFailure(edgeNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, @NotNull TbCallback callback, Throwable throwable) {
        log.error("Can't push to edge updates, edgeNotificationMsg [{}]", edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }

}
