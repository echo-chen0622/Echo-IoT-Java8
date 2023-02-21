package org.echoiot.server.service.edge.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainConnectionInfo;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.service.edge.rpc.constructor.*;
import org.echoiot.server.service.entitiy.TbNotificationEntityService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.echoiot.server.service.state.DeviceStateService;
import org.echoiot.server.service.telemetry.TelemetrySubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class BaseEdgeProcessor {

    protected static final int DEFAULT_PAGE_SIZE = 100;

    @Resource
    protected TelemetrySubscriptionService tsSubService;

    @Resource
    protected TbNotificationEntityService notificationEntityService;

    @Resource
    protected RuleChainService ruleChainService;

    @Resource
    protected AlarmService alarmService;

    @Resource
    protected DeviceService deviceService;

    @Resource
    protected TbDeviceProfileCache deviceProfileCache;

    @Resource
    protected TbAssetProfileCache assetProfileCache;

    @Resource
    protected DashboardService dashboardService;

    @Resource
    protected AssetService assetService;

    @Resource
    protected EntityViewService entityViewService;

    @Resource
    protected TenantService tenantService;

    @Resource
    protected EdgeService edgeService;

    @Resource
    protected CustomerService customerService;

    @Resource
    protected UserService userService;

    @Resource
    protected DeviceProfileService deviceProfileService;

    @Resource
    protected AssetProfileService assetProfileService;

    @Resource
    protected RelationService relationService;

    @Resource
    protected DeviceCredentialsService deviceCredentialsService;

    @Resource
    protected AttributesService attributesService;

    @Resource
    protected TbClusterService tbClusterService;

    @Resource
    protected DeviceStateService deviceStateService;

    @Resource
    protected EdgeEventService edgeEventService;

    @Resource
    protected WidgetsBundleService widgetsBundleService;

    @Resource
    protected WidgetTypeService widgetTypeService;

    @Resource
    protected OtaPackageService otaPackageService;

    @Resource
    protected QueueService queueService;

    @Resource
    protected PartitionService partitionService;

    @Resource
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @Resource
    protected DataValidator<Device> deviceValidator;

    @Resource
    protected EdgeMsgConstructor edgeMsgConstructor;

    @Resource
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @Resource
    protected RuleChainMsgConstructor ruleChainMsgConstructor;

    @Resource
    protected AlarmMsgConstructor alarmMsgConstructor;

    @Resource
    protected DeviceMsgConstructor deviceMsgConstructor;

    @Resource
    protected AssetMsgConstructor assetMsgConstructor;

    @Resource
    protected EntityViewMsgConstructor entityViewMsgConstructor;

    @Resource
    protected DashboardMsgConstructor dashboardMsgConstructor;

    @Resource
    protected RelationMsgConstructor relationMsgConstructor;

    @Resource
    protected UserMsgConstructor userMsgConstructor;

    @Resource
    protected CustomerMsgConstructor customerMsgConstructor;

    @Resource
    protected DeviceProfileMsgConstructor deviceProfileMsgConstructor;

    @Resource
    protected AssetProfileMsgConstructor assetProfileMsgConstructor;

    @Resource
    protected WidgetsBundleMsgConstructor widgetsBundleMsgConstructor;

    @Resource
    protected WidgetTypeMsgConstructor widgetTypeMsgConstructor;

    @Resource
    protected AdminSettingsMsgConstructor adminSettingsMsgConstructor;

    @Resource
    protected OtaPackageMsgConstructor otaPackageMsgConstructor;

    @Resource
    protected QueueMsgConstructor queueMsgConstructor;

    @Resource
    protected DbCallbackExecutorService dbCallbackExecutorService;

    @NotNull
    protected ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                   EdgeId edgeId,
                                                   EdgeEventType type,
                                                   EdgeEventActionType action,
                                                   EntityId entityId,
                                                   JsonNode body) {
        log.debug("Pushing event to edge queue. tenantId [{}], edgeId [{}], type[{}], " +
                        "action [{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        @NotNull EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallbackExecutorService);
    }

    @NotNull
    protected ListenableFuture<Void> processActionForAllEdges(TenantId tenantId, EdgeEventType type, EdgeEventActionType actionType, EntityId entityId) {
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<TenantId> tenantsIds;
            do {
                tenantsIds = tenantService.findTenantsIds(pageLink);
                for (TenantId tenantId1 : tenantsIds.getData()) {
                    futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, entityId, null));
                }
                pageLink = pageLink.nextPageLink();
            } while (tenantsIds.hasNext());
        } else {
            futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId, null);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @NotNull
    protected List<ListenableFuture<Void>> processActionForAllEdgesByTenantId(TenantId tenantId,
                                                                              EdgeEventType type,
                                                                              EdgeEventActionType actionType,
                                                                              EntityId entityId,
                                                                              JsonNode body) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<Edge> pageData;
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (@NotNull Edge edge : pageData.getData()) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, body));
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return futures;
    }

    @NotNull
    protected UpdateMsgType getUpdateMsgType(@NotNull EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ASSIGNED_TO_EDGE:
            case RELATION_ADD_OR_UPDATE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
            case RELATION_DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    protected ListenableFuture<Void> processEntityNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        @NotNull EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        @NotNull EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        @NotNull EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type,
                                                                               new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        @Nullable EdgeId edgeId = safeGetEdgeId(edgeNotificationMsg);
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
            case DELETED:
                if (edgeId != null) {
                    return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                } else {
                    return pushNotificationToAllRelatedEdges(tenantId, entityId, type, actionType);
                }
            case ASSIGNED_TO_EDGE:
            case UNASSIGNED_FROM_EDGE:
                @NotNull ListenableFuture<Void> future = saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                return Futures.transformAsync(future, unused -> {
                    if (type.equals(EdgeEventType.RULE_CHAIN)) {
                        return updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, dbCallbackExecutorService);
            default:
                return Futures.immediateFuture(null);
        }
    }

    @Nullable
    private EdgeId safeGetEdgeId(@NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        if (edgeNotificationMsg.getEdgeIdMSB() != 0 && edgeNotificationMsg.getEdgeIdLSB() != 0) {
            return new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
        } else {
            return null;
        }
    }

    @NotNull
    private ListenableFuture<Void> pushNotificationToAllRelatedEdges(TenantId tenantId, EntityId entityId, EdgeEventType type, EdgeEventActionType actionType) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EdgeId> pageData;
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (EdgeId relatedEdgeId : pageData.getData()) {
                    futures.add(saveEdgeEvent(tenantId, relatedEdgeId, type, actionType, entityId, null));
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @NotNull
    private ListenableFuture<Void> updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (@NotNull RuleChain ruleChain : pageData.getData()) {
                    if (!ruleChain.getId().equals(processingRuleChainId)) {
                        List<RuleChainConnectionInfo> connectionInfos =
                                ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
                        if (connectionInfos != null && !connectionInfos.isEmpty()) {
                            for (@NotNull RuleChainConnectionInfo connectionInfo : connectionInfos) {
                                if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                                    futures.add(saveEdgeEvent(tenantId,
                                            edgeId,
                                            EdgeEventType.RULE_CHAIN_METADATA,
                                            EdgeEventActionType.UPDATED,
                                            ruleChain.getId(),
                                            null));
                                }
                            }
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    protected ListenableFuture<Void> processEntityNotificationForAllEdges(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        @NotNull EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        @NotNull EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        @NotNull EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
            case CREDENTIALS_UPDATED: // used by USER entity
                return processActionForAllEdges(tenantId, type, actionType, entityId);
            default:
                return Futures.immediateFuture(null);
        }
    }

    @Nullable
    protected EntityId constructEntityId(String entityTypeStr, long entityIdMSB, long entityIdLSB) {
        @NotNull EntityType entityType = EntityType.valueOf(entityTypeStr);
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityIdMSB, entityIdLSB));
            case ASSET:
                return new AssetId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityIdMSB, entityIdLSB));
            case DASHBOARD:
                return new DashboardId(new UUID(entityIdMSB, entityIdLSB));
            case TENANT:
                return TenantId.fromUUID(new UUID(entityIdMSB, entityIdLSB));
            case CUSTOMER:
                return new CustomerId(new UUID(entityIdMSB, entityIdLSB));
            case USER:
                return new UserId(new UUID(entityIdMSB, entityIdLSB));
            case EDGE:
                return new EdgeId(new UUID(entityIdMSB, entityIdLSB));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. entityIdMSB [{}], entityIdLSB [{}]",
                        entityTypeStr, entityIdMSB, entityIdLSB);
                return null;
        }
    }
}
