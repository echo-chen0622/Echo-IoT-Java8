package org.echoiot.server.service.edge.rpc.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.DataType;
import org.echoiot.server.common.data.relation.*;
import org.echoiot.server.common.data.widget.WidgetType;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.entityview.TbEntityViewService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.state.DefaultDeviceStateService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeRequestsService implements EdgeRequestsService {

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Resource
    private EdgeEventService edgeEventService;

    @Resource
    private AttributesService attributesService;

    @Resource
    private RelationService relationService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private AssetService assetService;

    @Lazy
    @Resource
    private TbEntityViewService entityViewService;

    @Resource
    private DeviceProfileService deviceProfileService;

    @Resource
    private AssetProfileService assetProfileService;

    @Resource
    private WidgetsBundleService widgetsBundleService;

    @Resource
    private WidgetTypeService widgetTypeService;

    @Resource
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Resource
    private TbClusterService tbClusterService;

    @Override
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() == 0 || ruleChainMetadataRequestMsg.getRuleChainIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        RuleChainId ruleChainId =
                new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(),
                             EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
    }

    @Override
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        @org.jetbrains.annotations.Nullable final EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        if (type == null) {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            return Futures.immediateFuture(null);
        }
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributesRequestMsg.getScope();
        ListenableFuture<List<AttributeKvEntry>> findAttrFuture = attributesService.findAll(tenantId, entityId, scope);
        Futures.addCallback(findAttrFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                if (ssAttributes == null || ssAttributes.isEmpty()) {
                    log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                            edge.getName(),
                            entityId.getEntityType(),
                            entityId.getId());
                    futureToSet.set(null);
                    return;
                }

                try {
                    Map<String, Object> entityData = new HashMap<>();
                    ObjectNode attributes = JacksonUtil.OBJECT_MAPPER.createObjectNode();
                    for (AttributeKvEntry attr : ssAttributes) {
                        if (DefaultDeviceStateService.PERSISTENT_ATTRIBUTES.contains(attr.getKey())
                                && !DefaultDeviceStateService.INACTIVITY_TIMEOUT.equals(attr.getKey())) {
                            continue;
                        }
                        if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getBooleanValue().get());
                        } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getDoubleValue().get());
                        } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getLongValue().get());
                        } else {
                            attributes.put(attr.getKey(), attr.getValueAsString());
                        }
                    }
                    entityData.put("kv", attributes);
                    entityData.put("scope", scope);
                    JsonNode body = JacksonUtil.OBJECT_MAPPER.valueToTree(entityData);
                    log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, body);
                    ListenableFuture<Void> future = saveEdgeEvent(tenantId, edge.getId(), type, EdgeEventActionType.ATTRIBUTES_UPDATED, entityId, body);
                    Futures.addCallback(future, new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void unused) {
                            futureToSet.set(null);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            String errMsg = String.format("[%s] Failed to save edge event [%s]", edge.getId(), attributesRequestMsg);
                            log.error(errMsg, throwable);
                            futureToSet.setException(new RuntimeException(errMsg, throwable));
                        }
                    }, dbCallbackExecutorService);
                } catch (Exception e) {
                    String errMsg = String.format("[%s] Failed to save attribute updates to the edge [%s]", edge.getId(), attributesRequestMsg);
                    log.error(errMsg, e);
                    futureToSet.setException(new RuntimeException(errMsg, e));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("[%s] Can't find attributes [%s]", edge.getId(), attributesRequestMsg);
                log.error(errMsg, t);
                futureToSet.setException(new RuntimeException(errMsg, t));
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg) {
        log.trace("[{}] processRelationRequestMsg [{}][{}]", tenantId, edge.getName(), relationRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(relationsListFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (relationsList != null && !relationsList.isEmpty()) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        futures.add(saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                JacksonUtil.OBJECT_MAPPER.valueToTree(relation)));
                                    }
                                } catch (Exception e) {
                                    String errMsg = String.format("[%s] Exception during loading relation [%s] to edge on sync!", edge.getId(), relation);
                                    log.error(errMsg, e);
                                    futureToSet.setException(new RuntimeException(errMsg, e));
                                    return;
                                }
                            }
                        }
                        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<Void> voids) {
                                futureToSet.set(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                String errMsg = String.format("[%s] Exception during saving edge events [%s]!", edge.getId(), relationRequestMsg);
                                log.error(errMsg, throwable);
                                futureToSet.setException(new RuntimeException(errMsg, throwable));
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        futureToSet.set(null);
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("[%s] Can't find relation by query. Entity id [%s]!", tenantId, entityId);
                log.error(errMsg, t);
                futureToSet.setException(new RuntimeException(errMsg, t));
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge,
                                                                       EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        log.trace("[{}] processDeviceCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), deviceCredentialsRequestMsg);
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() == 0 || deviceCredentialsRequestMsg.getDeviceIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE,
                EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
    }

    @Override
    public ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        log.trace("[{}] processUserCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), userCredentialsRequestMsg);
        if (userCredentialsRequestMsg.getUserIdMSB() == 0 || userCredentialsRequestMsg.getUserIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER,
                EdgeEventActionType.CREDENTIALS_UPDATED, userId, null);
    }

    @Override
    public ListenableFuture<Void> processWidgetBundleTypesRequestMsg(TenantId tenantId, Edge edge,
                                                                     WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg) {
        log.trace("[{}] processWidgetBundleTypesRequestMsg [{}][{}]", tenantId, edge.getName(), widgetBundleTypesRequestMsg);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (widgetBundleTypesRequestMsg.getWidgetBundleIdMSB() != 0 && widgetBundleTypesRequestMsg.getWidgetBundleIdLSB() != 0) {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetBundleTypesRequestMsg.getWidgetBundleIdMSB(), widgetBundleTypesRequestMsg.getWidgetBundleIdLSB()));
            WidgetsBundle widgetsBundleById = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
            if (widgetsBundleById != null) {
                List<WidgetType> widgetTypesToPush =
                        widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(widgetsBundleById.getTenantId(), widgetsBundleById.getAlias());
                for (WidgetType widgetType : widgetTypesToPush) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGET_TYPE, EdgeEventActionType.ADDED, widgetType.getId(), null));
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    public ListenableFuture<Void> processEntityViewsRequestMsg(TenantId tenantId, Edge edge, EntityViewsRequestMsg entityViewsRequestMsg) {
        log.trace("[{}] processEntityViewsRequestMsg [{}][{}]", tenantId, edge.getName(), entityViewsRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(entityViewsRequestMsg.getEntityType()),
                new UUID(entityViewsRequestMsg.getEntityIdMSB(), entityViewsRequestMsg.getEntityIdLSB()));
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<EntityView> entityViews) {
                if (entityViews == null || entityViews.isEmpty()) {
                    futureToSet.set(null);
                    return;
                }
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                for (EntityView entityView : entityViews) {
                    ListenableFuture<Boolean> future = relationService.checkRelationAsync(tenantId, edge.getId(), entityView.getId(),
                                                                                          EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
                    futures.add(Futures.transformAsync(future, result -> {
                        if (Boolean.TRUE.equals(result)) {
                            return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                                    EdgeEventActionType.ADDED, entityView.getId(), null);
                        } else {
                            return Futures.immediateFuture(null);
                        }
                    }, dbCallbackExecutorService));
                }
                Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> result) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading relation to edge on sync!", t);
                        futureToSet.setException(t);
                    }
                }, dbCallbackExecutorService);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't find entity views by entity id [{}]", tenantId, entityId, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    private ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                 EdgeId edgeId,
                                                 EdgeEventType type,
                                                 EdgeEventActionType action,
                                                 EntityId entityId,
                                                 JsonNode body) {
        log.trace("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallbackExecutorService);
    }

}
