package org.echoiot.server.service.entitiy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.service.action.EntityActionService;
import org.echoiot.server.service.gateway_device.GatewayNotificationsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTbNotificationEntityService implements TbNotificationEntityService {

    @NotNull
    private final EntityActionService entityActionService;
    @NotNull
    private final TbClusterService tbClusterService;
    @NotNull
    private final GatewayNotificationsService gatewayNotificationsService;

    @Override
    public <I extends EntityId> void logEntityAction(TenantId tenantId, @NotNull I entityId, @NotNull ActionType actionType,
                                                     User user, Exception e, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, null, null, actionType, user, e, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, @NotNull I entityId, E entity,
                                                                        @NotNull ActionType actionType, User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, null, actionType, user, null, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, @NotNull I entityId, E entity,
                                                                        @NotNull ActionType actionType, User user, Exception e,
                                                                        Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, null, actionType, user, e, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, @NotNull I entityId, E entity, CustomerId customerId,
                                                                        @NotNull ActionType actionType, User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, null, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, @NotNull I entityId, E entity,
                                                                        CustomerId customerId, @NotNull ActionType actionType,
                                                                        @Nullable User user, @Nullable Exception e, Object... additionalInfo) {
        if (user != null) {
            entityActionService.logEntityAction(user, entityId, entity, customerId, actionType, e, additionalInfo);
        } else if (e == null) {
            entityActionService.pushEntityActionToRuleEngine(entityId, entity, tenantId, customerId, actionType, null, additionalInfo);
        }
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyDeleteEntity(TenantId tenantId, @NotNull I entityId, E entity,
                                                                           CustomerId customerId, @NotNull ActionType actionType,
                                                                           List<EdgeId> relatedEdgeIds,
                                                                           User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);
        sendDeleteNotificationMsg(tenantId, entityId, relatedEdgeIds, null);
    }

    @Override
    public void notifyDeleteAlarm(TenantId tenantId, @NotNull Alarm alarm, @NotNull EntityId originatorId, CustomerId customerId,
                                  List<EdgeId> relatedEdgeIds, User user, String body, Object... additionalInfo) {
        logEntityAction(tenantId, originatorId, alarm, customerId, ActionType.DELETED, user, additionalInfo);
        sendAlarmDeleteNotificationMsg(tenantId, alarm, relatedEdgeIds, body);
    }

    @Override
    public void notifyDeleteRuleChain(TenantId tenantId, @NotNull RuleChain ruleChain, List<EdgeId> relatedEdgeIds, User user) {
        RuleChainId ruleChainId = ruleChain.getId();
        logEntityAction(tenantId, ruleChainId, ruleChain, null, ActionType.DELETED, user, null, ruleChainId.toString());
        if (RuleChainType.EDGE.equals(ruleChain.getType())) {
            sendDeleteNotificationMsg(tenantId, ruleChainId, relatedEdgeIds, null);
        }
    }

    @Override
    public <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventActionType edgeEventActionType) {
        sendEntityNotificationMsg(tenantId, entityId, edgeEventActionType);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToCustomer(TenantId tenantId, @NotNull I entityId,
                                                                                               CustomerId customerId, E entity,
                                                                                               @NotNull ActionType actionType,
                                                                                               User user, boolean sendToEdge,
                                                                                               Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);

        if (sendToEdge) {
            sendEntityNotificationMsg(tenantId, entityId, edgeTypeByActionType(actionType), JacksonUtil.toString(customerId));
        }
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToEdge(TenantId tenantId, @NotNull I entityId,
                                                                                           CustomerId customerId, EdgeId edgeId,
                                                                                           E entity, @NotNull ActionType actionType,
                                                                                           User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);
        sendEntityAssignToEdgeNotificationMsg(tenantId, edgeId, entityId, edgeTypeByActionType(actionType));
    }

    @Override
    public void notifyCreateOrUpdateTenant(@NotNull Tenant tenant, ComponentLifecycleEvent event) {
        tbClusterService.onTenantChange(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), event);
    }

    @Override
    public void notifyDeleteTenant(@NotNull Tenant tenant) {
        tbClusterService.onTenantDelete(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), ComponentLifecycleEvent.DELETED);
    }

    @Override
    public void notifyCreateOrUpdateDevice(TenantId tenantId, @NotNull DeviceId deviceId, CustomerId customerId,
                                           Device device, Device oldDevice, @NotNull ActionType actionType,
                                           User user, Object... additionalInfo) {
        tbClusterService.onDeviceUpdated(device, oldDevice);
        logEntityAction(tenantId, deviceId, device, customerId, actionType, user, additionalInfo);
    }

    @Override
    public void notifyDeleteDevice(TenantId tenantId, @NotNull DeviceId deviceId, CustomerId customerId, Device device,
                                   List<EdgeId> relatedEdgeIds, User user, Object... additionalInfo) {
        gatewayNotificationsService.onDeviceDeleted(device);
        tbClusterService.onDeviceDeleted(device, null);

        notifyDeleteEntity(tenantId, deviceId, device, customerId, ActionType.DELETED, relatedEdgeIds, user, additionalInfo);
    }

    @Override
    public void notifyUpdateDeviceCredentials(TenantId tenantId, @NotNull DeviceId deviceId, CustomerId customerId, Device device,
                                              @NotNull DeviceCredentials deviceCredentials, User user) {
        tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceCredentials.getDeviceId(), deviceCredentials), null);
        sendEntityNotificationMsg(tenantId, deviceId, EdgeEventActionType.CREDENTIALS_UPDATED);
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.CREDENTIALS_UPDATED, user, deviceCredentials);
    }

    @Override
    public void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, @NotNull DeviceId deviceId, CustomerId customerId,
                                           @NotNull Device device, @NotNull Tenant tenant, User user, Object... additionalInfo) {
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.ASSIGNED_TO_TENANT, user, additionalInfo);
        pushAssignedFromNotification(tenant, newTenantId, device);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyCreateOrUpdateEntity(TenantId tenantId, @NotNull I entityId, E entity,
                                                                                   CustomerId customerId, @NotNull ActionType actionType,
                                                                                   User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);
        if (actionType == ActionType.UPDATED) {
            sendEntityNotificationMsg(tenantId, entityId, EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public void notifyCreateOrUpdateOrDeleteEdge(TenantId tenantId, @NotNull EdgeId edgeId, CustomerId customerId, Edge edge,
                                                 @NotNull ActionType actionType, User user, Object... additionalInfo) {
        ComponentLifecycleEvent lifecycleEvent;
        switch (actionType) {
            case ADDED:
                lifecycleEvent = ComponentLifecycleEvent.CREATED;
                break;
            case UPDATED:
                lifecycleEvent = ComponentLifecycleEvent.UPDATED;
                break;
            case DELETED:
                lifecycleEvent = ComponentLifecycleEvent.DELETED;
                break;
            default:
                throw new IllegalArgumentException("Unknown actionType: " + actionType);
        }
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, edgeId, lifecycleEvent);
        logEntityAction(tenantId, edgeId, edge, customerId, actionType, user, additionalInfo);
    }

    @Override
    public void notifyCreateOrUpdateAlarm(@NotNull Alarm alarm, @NotNull ActionType actionType, User user, Object... additionalInfo) {
        logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarm, alarm.getCustomerId(), actionType, user, additionalInfo);
        sendEntityNotificationMsg(alarm.getTenantId(), alarm.getId(), edgeTypeByActionType(actionType));
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyCreateOrUpdateOrDelete(TenantId tenantId, CustomerId customerId,
                                                                                     @NotNull I entityId, E entity, User user,
                                                                                     @NotNull ActionType actionType, boolean sendNotifyMsgToEdge, Exception e,
                                                                                     Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, e, additionalInfo);
        if (sendNotifyMsgToEdge) {
            sendEntityNotificationMsg(tenantId, entityId, edgeTypeByActionType(actionType));
        }
    }

    @Override
    public void notifyRelation(TenantId tenantId, CustomerId customerId, @NotNull EntityRelation relation, User user,
                               @NotNull ActionType actionType, Object... additionalInfo) {
        logEntityAction(tenantId, relation.getFrom(), null, customerId, actionType, user, additionalInfo);
        logEntityAction(tenantId, relation.getTo(), null, customerId, actionType, user, additionalInfo);
        if (!EntityType.EDGE.equals(relation.getFrom().getEntityType()) && !EntityType.EDGE.equals(relation.getTo().getEntityType())) {
            sendNotificationMsgToEdge(tenantId, null, null, JacksonUtil.toString(relation),
                                      EdgeEventType.RELATION, edgeTypeByActionType(actionType));
        }
    }

    private void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action) {
        sendEntityNotificationMsg(tenantId, entityId, action, null);
    }

    private void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action, String body) {
        sendNotificationMsgToEdge(tenantId, null, entityId, body, null, action);
    }

    private void sendAlarmDeleteNotificationMsg(TenantId tenantId, @NotNull Alarm alarm, List<EdgeId> edgeIds, String body) {
        sendDeleteNotificationMsg(tenantId, alarm.getId(), edgeIds, body);
    }

    private void sendDeleteNotificationMsg(TenantId tenantId, EntityId entityId, @Nullable List<EdgeId> edgeIds, String body) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, null, EdgeEventActionType.DELETED);
            }
        }
    }

    private void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, action);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                           EdgeEventType type, EdgeEventActionType action) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action);
    }

    private void pushAssignedFromNotification(@NotNull Tenant currentTenant, TenantId newTenantId, @NotNull Device assignedDevice) {
        @Nullable String data = JacksonUtil.toString(JacksonUtil.valueToTree(assignedDevice));
        if (data != null) {
            @NotNull TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(),
                                                assignedDevice.getCustomerId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
            tbClusterService.pushMsgToRuleEngine(newTenantId, assignedDevice.getId(), tbMsg, null);
        }
    }

    @NotNull
    private TbMsgMetaData getMetaDataForAssignedFrom(@NotNull Tenant tenant) {
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assignedFromTenantId", tenant.getId().getId().toString());
        metaData.putValue("assignedFromTenantName", tenant.getName());
        return metaData;
    }

    @Nullable
    public static EdgeEventActionType edgeTypeByActionType(@NotNull ActionType actionType) {
        switch (actionType) {
            case ADDED:
                return EdgeEventActionType.ADDED;
            case UPDATED:
                return EdgeEventActionType.UPDATED;
            case ALARM_ACK:
                return EdgeEventActionType.ALARM_ACK;
            case ALARM_CLEAR:
                return EdgeEventActionType.ALARM_CLEAR;
            case DELETED:
                return EdgeEventActionType.DELETED;
            case RELATION_ADD_OR_UPDATE:
                return EdgeEventActionType.RELATION_ADD_OR_UPDATE;
            case RELATION_DELETED:
                return EdgeEventActionType.RELATION_DELETED;
            case ASSIGNED_TO_CUSTOMER:
                return EdgeEventActionType.ASSIGNED_TO_CUSTOMER;
            case UNASSIGNED_FROM_CUSTOMER:
                return EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER;
            case ASSIGNED_TO_EDGE:
                return EdgeEventActionType.ASSIGNED_TO_EDGE;
            case UNASSIGNED_FROM_EDGE:
                return EdgeEventActionType.UNASSIGNED_FROM_EDGE;
            default:
                return null;
        }
    }
}
