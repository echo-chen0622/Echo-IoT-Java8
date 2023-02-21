package org.echoiot.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.echoiot.server.dao.audit.AuditLogService;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.echoiot.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    protected final String msgErrorPermission = "You don't have permission to perform this operation!";
    protected final String msgErrorShouldBeSpecified = "should be specified";
    protected final String msgErrorNotFound = "Requested item wasn't found!";


    protected void testNotifyEntityAllOneTime(@NotNull HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                              TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                              @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeRelation(@NotNull EntityRelation relation,
                                                      TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                      @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.isNull(), Mockito.any(), Mockito.eq(EdgeEventType.RELATION),
                Mockito.eq(edgeTypeByActionType(actionType)));
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(relation.getTo());
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        matcherOriginatorId = argument -> argument.equals(relation.getFrom());
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllManyRelation(@NotNull EntityRelation relation,
                                                   TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                   @NotNull ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.isNull(), Mockito.any(), Mockito.eq(EdgeEventType.RELATION),
                Mockito.eq(edgeTypeByActionType(actionType)));
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(relation.getFrom().getClass());
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId,
                userName, actionType, cntTime * 2, 1);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, new Tenant(), cntTime * 3);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(@NotNull HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityActionEntityEqClass(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNeverMsgToEdgeServiceOneTime(HasName entity, @NotNull EntityId entityId, TenantId tenantId,
                                                                @NotNull ActionType actionType) {
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, 1);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeMsgToEdgeServiceNever(@NotNull HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                                                TenantId tenantId, CustomerId customerId, UserId userId,
                                                                String userName, @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNever(@NotNull HasName entity, @NotNull HasName originator,
                                                                     TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                                     @NotNull ActionType actionType, int cntTime, Object... additionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(@NotNull HasName entity, @NotNull HasName originator,
                                                                           TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                                           ActionType actionType, @NotNull ActionType actionTypeEdge,
                                                                           int cntTime, int cntTimeEdge, int cntTimeRuleEngine, Object... additionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(tenantId, actionTypeEdge, cntTimeEdge);
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfoClass(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeRuleEngine);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(@NotNull HasName entity, @NotNull HasName originator,
                                                                                            TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                                                            ActionType actionType, @NotNull ActionType actionTypeEdge, int cntTime, int cntTimeEdge, int cntAdditionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(tenantId, actionTypeEdge, cntTimeEdge);
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeEdge);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(@NotNull HasName entity, @NotNull HasName originator,
                                                                                      TenantId tenantId, @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                                                      @NotNull ActionType actionType, int cntTime, int cntAdditionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTime(@NotNull HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(@NotNull HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                               @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventMany(@NotNull HasName entity, @NotNull HasName originator,
                                                                       TenantId tenantId, @Nullable CustomerId customerId,
                                                                       @Nullable UserId userId, String userName, ActionType actionType,
                                                                       @NotNull ActionType actionTypeEdge,
                                                                       int cntTime, int cntTimeEdge, int cntTimeRuleEngine,
                                                                       int cntAdditionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionTypeEdge, cntTimeEdge);
        @NotNull ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeRuleEngine);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
    }

    protected void testNotifyEntityMsgToEdgePushMsgToCoreOneTime(HasName entity, @NotNull EntityId entityId, EntityId originatorId,
                                                                 TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                 @NotNull ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        tesPushMsgToCoreTime(cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityEqualsOneTimeServiceNeverError(@NotNull HasName entity, TenantId tenantId,
                                                                  UserId userId, String userName, @NotNull ActionType actionType, @NotNull Exception exp,
                                                                  Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNeverWithActionType(entity_originator_NULL_UUID, actionType);
        @NotNull ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exp.getMessage())
                                                                       & argument.getClass().equals(exp.getClass());
        testLogEntityActionErrorAdditionalInfo(matcherEntityEquals, entity_originator_NULL_UUID, tenantId, customer_NULL_UUID, userId,
                userName, actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(entity_originator_NULL_UUID);
    }

    protected void testNotifyEntityIsNullOneTimeEdgeServiceNeverError(@NotNull HasName entity, TenantId tenantId,
                                                                      UserId userId, String userName, @NotNull ActionType actionType, @NotNull Exception exp,
                                                                      Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNeverWithActionType(entity_originator_NULL_UUID, actionType);
        @NotNull ArgumentMatcher<HasName> matcherEntityIsNull = Objects::isNull;
        @NotNull ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exp.getMessage()) &
                                                                       argument.getClass().equals(exp.getClass());
        testLogEntityActionErrorAdditionalInfo(matcherEntityIsNull, entity_originator_NULL_UUID, tenantId, customer_NULL_UUID,
                userId, userName, actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(entity_originator_NULL_UUID);
    }

    protected void testNotifyEntityNever(EntityId entityId, @NotNull HasName entity) {
        entityId = entityId == null ? createEntityId_NULL_UUID(entity) : entityId;
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    private void testNotificationMsgToEdgeServiceNeverWithActionType(@NotNull EntityId entityId, @NotNull ActionType actionType) {
        @Nullable EdgeEventActionType edgeEventActionType = ActionType.CREDENTIALS_UPDATED.equals(actionType) ?
                EdgeEventActionType.CREDENTIALS_UPDATED : edgeTypeByActionType(actionType);
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.eq(edgeEventActionType));
    }

    private void testNotificationMsgToEdgeServiceNever(@NotNull EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionNever(@NotNull EntityId entityId, @Nullable HasName entity) {
        @NotNull ArgumentMatcher<HasName> matcherEntity = entity == null ? Objects::isNull :
                argument -> argument.getClass().equals(entity.getClass());
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.argThat(matcherEntity),
                Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineNever(@NotNull EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    protected void testBroadcastEntityStateChangeEventNever(@NotNull EntityId entityId) {
        Mockito.verify(tbClusterService, never()).broadcastEntityStateChangeEvent(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private void testPushMsgToRuleEngineTime(ArgumentMatcher<EntityId> matcherOriginatorId, TenantId tenantId, @NotNull HasName entity, int cntTime) {
        tenantId = tenantId.isNullUid() && ((HasTenantId) entity).getTenantId() != null ? ((HasTenantId) entity).getTenantId() : tenantId;
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                                                                             Mockito.argThat(matcherOriginatorId), Mockito.any(TbMsg.class), Mockito.isNull());
    }

    private void testNotificationMsgToEdgeServiceTime(@NotNull EntityId entityId, TenantId tenantId, @NotNull ActionType actionType, int cntTime) {
        @Nullable EdgeEventActionType edgeEventActionType = ActionType.CREDENTIALS_UPDATED.equals(actionType) ?
                EdgeEventActionType.CREDENTIALS_UPDATED : edgeTypeByActionType(actionType);
        @NotNull ArgumentMatcher<EntityId> matcherEntityId = cntTime == 1 ? argument -> argument.equals(entityId) :
                argument -> argument.getClass().equals(entityId.getClass());
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.argThat(matcherEntityId), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeEventActionType));
    }

    private void testSendNotificationMsgToEdgeServiceTimeEntityEqAny(TenantId tenantId, @NotNull ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.any(EntityId.class), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

    protected void testBroadcastEntityStateChangeEventTime(@NotNull EntityId entityId, @Nullable TenantId tenantId, int cntTime) {
        @NotNull ArgumentMatcher<TenantId> matcherTenantIdId = cntTime > 1 || tenantId == null ? argument -> argument.getClass().equals(TenantId.class) :
                argument -> argument.equals(tenantId) ;
        Mockito.verify(tbClusterService, times(cntTime)).broadcastEntityStateChangeEvent(Mockito.argThat(matcherTenantIdId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private void tesPushMsgToCoreTime(int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToCore(Mockito.any(ToDeviceActorNotificationMsg.class), Mockito.isNull());
    }

    private void testLogEntityAction(@Nullable HasName entity, EntityId originatorId, TenantId tenantId,
                                     @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                     ActionType actionType, int cntTime, Object... additionalInfo) {
        @NotNull ArgumentMatcher<HasName> matcherEntityEquals = entity == null ? Objects::isNull : argument -> argument.toString().equals(entity.toString());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionEntityEqClass(@NotNull HasName entity, EntityId originatorId, TenantId tenantId,
                                                  @Nullable CustomerId customerId, @Nullable UserId userId, String userName,
                                                  ActionType actionType, int cntTime, Object... additionalInfo) {
        @NotNull ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        @NotNull ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        @NotNull ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionAdditionalInfo(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                   TenantId tenantId, ArgumentMatcher<CustomerId> matcherCustomerId,
                                                   ArgumentMatcher<UserId> matcherUserId, String userName, ActionType actionType,
                                                   int cntTime, @NotNull List<ArgumentMatcher<Object>> matcherAdditionalInfos) {
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)));
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)));
                break;
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)),
                                Mockito.argThat(matcherAdditionalInfos.get(2)));
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull());
        }
    }

    private void testLogEntityActionAdditionalInfoAny(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                      TenantId tenantId, ArgumentMatcher<CustomerId> matcherCustomerId,
                                                      ArgumentMatcher<UserId> matcherUserId, String userName,
                                                      ActionType actionType, int cntTime, int cntAdditionalInfo) {
        switch (cntAdditionalInfo) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any());
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any(),
                                Mockito.any());
                break;
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any());
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull());
        }
    }

    private void testLogEntityActionErrorAdditionalInfo(ArgumentMatcher<HasName> matcherEntity, EntityId originatorId, TenantId tenantId,
                                                        CustomerId customerId, @Nullable UserId userId, String userName, ActionType actionType,
                                                        int cntTime, ArgumentMatcher<Exception> matcherError,
                                                        @NotNull List<ArgumentMatcher<Object>> matcherAdditionalInfos) {
        @NotNull ArgumentMatcher<UserId> matcherUserId = userId == null ? argument -> argument.getClass().equals(UserId.class) :
                argument -> argument.equals(userId);
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(matcherAdditionalInfos.get(0)));
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(0))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(1))));
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(0))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(1))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(2))));
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError));
        }
    }

    @NotNull
    private List<ArgumentMatcher<Object>> extractMatcherAdditionalInfo(@NotNull Object... additionalInfos) {
        @NotNull List<ArgumentMatcher<Object>> matcherAdditionalInfos = new ArrayList<>(additionalInfos.length);
        for (@NotNull Object additionalInfo : additionalInfos) {
            matcherAdditionalInfos.add(argument -> argument.equals(extractParameter(additionalInfo.getClass(), additionalInfo)));
        }
        return matcherAdditionalInfos;
    }

    @NotNull
    private List<ArgumentMatcher<Object>> extractMatcherAdditionalInfoClass(@NotNull Object... additionalInfos) {
        @NotNull List<ArgumentMatcher<Object>> matcherAdditionalInfos = new ArrayList<>(additionalInfos.length);
        for (@NotNull Object additionalInfo : additionalInfos) {
            matcherAdditionalInfos.add(argument -> argument.getClass().equals(extractParameter(additionalInfo.getClass(), additionalInfo).getClass()));
        }
        return matcherAdditionalInfos;
    }

    @Nullable
    private <T> T extractParameter(@NotNull Class<T> clazz, @Nullable Object additionalInfo) {
        @Nullable T result = null;
        if (additionalInfo != null) {
            @NotNull Object paramObject = additionalInfo;
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    protected EntityId createEntityId_NULL_UUID(@NotNull HasName entity) {
        return EntityIdFactory.getByTypeAndUuid(entityClassToEntityTypeName(entity), ModelConstants.NULL_UUID);
    }

    @NotNull
    protected String msgErrorFieldLength(String fieldName) {
        return fieldName + " length must be equal or less than 255";
    }

    @NotNull
    protected String msgErrorNoFound(String entityClassName, String assetIdStr) {
        return entityClassName + " with id [" + assetIdStr + "] is not found";
    }

    @NotNull
    private String entityClassToEntityTypeName(@NotNull HasName entity) {
        @NotNull String entityType =  entityClassToString(entity);
        return "SAVE_OTA_PACKAGE_INFO_REQUEST".equals(entityType) || "OTA_PACKAGE_INFO".equals(entityType)?
                EntityType.OTA_PACKAGE.name().toUpperCase(Locale.ENGLISH) : entityType;
    }

    @NotNull
    private String entityClassToString(@NotNull HasName entity) {
        @NotNull String className = entity.getClass().toString()
                                          .substring(entity.getClass().toString().lastIndexOf(".") + 1);
        @NotNull List str = className.chars()
                                     .mapToObj(x -> (Character.isUpperCase(x)) ? "_" + Character.toString(x) : Character.toString(x))
                                     .collect(Collectors.toList());
        return String.join("", str).toUpperCase(Locale.ENGLISH).substring(1);
    }
}
