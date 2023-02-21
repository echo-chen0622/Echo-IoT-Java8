package org.echoiot.server.dao.audit;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.audit.ActionStatus;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.audit.AuditLog;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.audit.sink.AuditLogSink;
import org.echoiot.server.dao.device.provision.ProvisionRequest;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogServiceImpl implements AuditLogService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final int INSERTS_PER_ENTRY = 3;

    @Resource
    private AuditLogLevelFilter auditLogLevelFilter;

    @Resource
    private AuditLogDao auditLogDao;

    @Resource
    private EntityService entityService;

    @Resource
    private AuditLogSink auditLogSink;

    @Resource
    private DataValidator<AuditLog> auditLogValidator;

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(@NotNull TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        return auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(@NotNull TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userId, "Incorrect userId" + userId);
        return auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(@NotNull TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateEntityId(entityId, INCORRECT_TENANT_ID + entityId);
        return auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(@NotNull TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), actionTypes, pageLink);
    }

    @Nullable
    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<List<Void>>
    logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, @NotNull I entityId, @Nullable E entity,
                    @NotNull ActionType actionType, @Nullable Exception e, Object... additionalInfo) {
        if (canLog(entityId.getEntityType(), actionType)) {
            JsonNode actionData = constructActionData(entityId, entity, actionType, additionalInfo);
            @NotNull ActionStatus actionStatus = ActionStatus.SUCCESS;
            String failureDetails = "";
            String entityName = "";
            if (entity != null) {
                entityName = entity.getName();
            } else {
                try {
                    entityName = entityService.fetchEntityNameAsync(tenantId, entityId).get();
                } catch (Exception ex) {
                }
            }
            if (e != null) {
                actionStatus = ActionStatus.FAILURE;
                failureDetails = getFailureStack(e);
            }
            if (actionType == ActionType.RPC_CALL) {
                String rpcErrorString = extractParameter(String.class, additionalInfo);
                if (!StringUtils.isEmpty(rpcErrorString)) {
                    actionStatus = ActionStatus.FAILURE;
                    failureDetails = rpcErrorString;
                }
            }
            return logAction(tenantId,
                    entityId,
                    entityName,
                    customerId,
                    userId,
                    userName,
                    actionType,
                    actionData,
                    actionStatus,
                    failureDetails);
        } else {
            return null;
        }
    }

    private <E extends HasName, I extends EntityId> JsonNode constructActionData(@NotNull I entityId, @Nullable E entity,
                                                                                 @NotNull ActionType actionType,
                                                                                 Object... additionalInfo) {
        ObjectNode actionData = JacksonUtil.newObjectNode();
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case RELATIONS_DELETED:
            case ASSIGNED_TO_TENANT:
                if (entity != null) {
                    ObjectNode entityNode = (ObjectNode) JacksonUtil.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    actionData.set("entity", entityNode);
                }
                if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainMetaData ruleChainMetaData = extractParameter(RuleChainMetaData.class, additionalInfo);
                    if (ruleChainMetaData != null) {
                        ObjectNode ruleChainMetaDataNode = (ObjectNode) JacksonUtil.valueToTree(ruleChainMetaData);
                        actionData.set("metadata", ruleChainMetaDataNode);
                    }
                }
                break;
            case DELETED:
            case ACTIVATED:
            case SUSPENDED:
            case CREDENTIALS_READ:
                @Nullable String strEntityId = extractParameter(String.class, additionalInfo);
                actionData.put("entityId", strEntityId);
                break;
            case ATTRIBUTES_UPDATED:
                actionData.put("entityId", entityId.toString());
                @Nullable String scope = extractParameter(String.class, 0, additionalInfo);
                @Nullable @SuppressWarnings("unchecked")
                List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                actionData.put("scope", scope);
                ObjectNode attrsNode = JacksonUtil.newObjectNode();
                if (attributes != null) {
                    for (@NotNull AttributeKvEntry attr : attributes) {
                        attrsNode.put(attr.getKey(), attr.getValueAsString());
                    }
                }
                actionData.set("attributes", attrsNode);
                break;
            case ATTRIBUTES_DELETED:
            case ATTRIBUTES_READ:
                actionData.put("entityId", entityId.toString());
                scope = extractParameter(String.class, 0, additionalInfo);
                actionData.put("scope", scope);
                @Nullable @SuppressWarnings("unchecked")
                List<String> keys = extractParameter(List.class, 1, additionalInfo);
                ArrayNode attrsArrayNode = actionData.putArray("attributes");
                if (keys != null) {
                    keys.forEach(attrsArrayNode::add);
                }
                break;
            case RPC_CALL:
                actionData.put("entityId", entityId.toString());
                @Nullable Boolean oneWay = extractParameter(Boolean.class, 1, additionalInfo);
                @Nullable String method = extractParameter(String.class, 2, additionalInfo);
                @Nullable String params = extractParameter(String.class, 3, additionalInfo);
                actionData.put("oneWay", oneWay);
                actionData.put("method", method);
                actionData.put("params", params);
                break;
            case CREDENTIALS_UPDATED:
                actionData.put("entityId", entityId.toString());
                DeviceCredentials deviceCredentials = extractParameter(DeviceCredentials.class, additionalInfo);
                actionData.set("credentials", JacksonUtil.valueToTree(deviceCredentials));
                break;
            case ASSIGNED_TO_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                @Nullable String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                @Nullable String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedCustomerId", strCustomerId);
                actionData.put("assignedCustomerName", strCustomerName);
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strCustomerId = extractParameter(String.class, 1, additionalInfo);
                strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedCustomerId", strCustomerId);
                actionData.put("unassignedCustomerName", strCustomerName);
                break;
            case RELATION_ADD_OR_UPDATE:
            case RELATION_DELETED:
                @Nullable EntityRelation relation = extractParameter(EntityRelation.class, 0, additionalInfo);
                actionData.set("relation", JacksonUtil.valueToTree(relation));
                break;
            case LOGIN:
            case LOGOUT:
            case LOCKOUT:
                @Nullable String clientAddress = extractParameter(String.class, 0, additionalInfo);
                @Nullable String browser = extractParameter(String.class, 1, additionalInfo);
                @Nullable String os = extractParameter(String.class, 2, additionalInfo);
                @Nullable String device = extractParameter(String.class, 3, additionalInfo);
                @Nullable String provider = extractParameter(String.class, 4, additionalInfo);
                actionData.put("clientAddress", clientAddress);
                actionData.put("browser", browser);
                actionData.put("os", os);
                actionData.put("device", device);
                if (StringUtils.hasText(provider)) {
                    actionData.put("provider", provider);
                }
                break;
            case PROVISION_SUCCESS:
            case PROVISION_FAILURE:
                ProvisionRequest request = extractParameter(ProvisionRequest.class, additionalInfo);
                if (request != null) {
                    actionData.set("provisionRequest", JacksonUtil.valueToTree(request));
                }
                break;
            case TIMESERIES_UPDATED:
                actionData.put("entityId", entityId.toString());
                @Nullable @SuppressWarnings("unchecked")
                List<TsKvEntry> updatedTimeseries = extractParameter(List.class, 0, additionalInfo);
                if (updatedTimeseries != null) {
                    ArrayNode result = actionData.putArray("timeseries");
                    updatedTimeseries.stream()
                            .collect(Collectors.groupingBy(TsKvEntry::getTs))
                            .forEach((k, v) -> {
                                ObjectNode element = JacksonUtil.newObjectNode();
                                element.put("ts", k);
                                ObjectNode values = element.putObject("values");
                                v.forEach(kvEntry -> values.put(kvEntry.getKey(), kvEntry.getValueAsString()));
                                result.add(element);
                            });
                }
                break;
            case TIMESERIES_DELETED:
                actionData.put("entityId", entityId.toString());
                @Nullable @SuppressWarnings("unchecked")
                List<String> timeseriesKeys = extractParameter(List.class, 0, additionalInfo);
                if (timeseriesKeys != null) {
                    ArrayNode timeseriesArrayNode = actionData.putArray("timeseries");
                    timeseriesKeys.forEach(timeseriesArrayNode::add);
                }
                actionData.put("startTs", extractParameter(Long.class, 1, additionalInfo));
                actionData.put("endTs", extractParameter(Long.class, 2, additionalInfo));
                break;
            case ASSIGNED_TO_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                @Nullable String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                @Nullable String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedEdgeId", strEdgeId);
                actionData.put("assignedEdgeName", strEdgeName);
                break;
            case UNASSIGNED_FROM_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strEdgeId = extractParameter(String.class, 1, additionalInfo);
                strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedEdgeId", strEdgeId);
                actionData.put("unassignedEdgeName", strEdgeName);
                break;
        }
        return actionData;
    }

    private <T> T extractParameter(@NotNull Class<T> clazz, Object... additionalInfo) {
        return extractParameter(clazz, 0, additionalInfo);
    }

    @Nullable
    private <T> T extractParameter(@NotNull Class<T> clazz, int index, @Nullable Object... additionalInfo) {
        @Nullable T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    private String getFailureStack(@NotNull Exception e) {
        @NotNull StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private boolean canLog(EntityType entityType, @NotNull ActionType actionType) {
        return auditLogLevelFilter.logEnabled(entityType, actionType);
    }

    @NotNull
    private AuditLog createAuditLogEntry(TenantId tenantId,
                                         EntityId entityId,
                                         String entityName,
                                         CustomerId customerId,
                                         UserId userId,
                                         String userName,
                                         ActionType actionType,
                                         JsonNode actionData,
                                         ActionStatus actionStatus,
                                         String actionFailureDetails) {
        @NotNull AuditLog result = new AuditLog();
        @NotNull UUID id = Uuids.timeBased();
        result.setId(new AuditLogId(id));
        result.setCreatedTime(Uuids.unixTimestamp(id));
        result.setTenantId(tenantId);
        result.setEntityId(entityId);
        result.setEntityName(entityName);
        result.setCustomerId(customerId);
        result.setUserId(userId);
        result.setUserName(userName);
        result.setActionType(actionType);
        result.setActionData(actionData);
        result.setActionStatus(actionStatus);
        result.setActionFailureDetails(actionFailureDetails);
        return result;
    }

    @NotNull
    private ListenableFuture<List<Void>> logAction(TenantId tenantId,
                                                   EntityId entityId,
                                                   String entityName,
                                                   CustomerId customerId,
                                                   UserId userId,
                                                   String userName,
                                                   ActionType actionType,
                                                   JsonNode actionData,
                                                   ActionStatus actionStatus,
                                                   String actionFailureDetails) {
        @NotNull AuditLog auditLogEntry = createAuditLogEntry(tenantId, entityId, entityName, customerId, userId, userName,
                                                              actionType, actionData, actionStatus, actionFailureDetails);
        log.trace("Executing logAction [{}]", auditLogEntry);
        try {
            auditLogValidator.validate(auditLogEntry, AuditLog::getTenantId);
        } catch (Exception e) {
            if (StringUtils.contains(e.getMessage(), "is malformed")) {
                auditLogEntry.setEntityName("MALFORMED");
            } else {
                return Futures.immediateFailedFuture(e);
            }
        }
        @NotNull List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        futures.add(auditLogDao.saveByTenantId(auditLogEntry));

        auditLogSink.logAction(auditLogEntry);

        return Futures.allAsList(futures);
    }

}
