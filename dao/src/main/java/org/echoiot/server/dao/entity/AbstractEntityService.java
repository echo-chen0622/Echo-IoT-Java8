package org.echoiot.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.relation.RelationService;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class AbstractEntityService {

    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Lazy
    @Resource
    protected RelationService relationService;

    @Lazy
    @Resource
    protected AlarmService alarmService;

    @Lazy
    @Resource
    protected EntityViewService entityViewService;

    @Lazy
    @Autowired(required = false)
    protected EdgeService edgeService;

    protected void createRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    protected void deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    protected void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        relationService.deleteEntityRelations(tenantId, entityId);
        log.trace("Executing deleteEntityAlarms [{}]", entityId);
        alarmService.deleteEntityAlarmRelations(tenantId, entityId);
    }

    @NotNull
    protected static Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

    public static final void checkConstraintViolation(Exception t, String constraintName, String constraintMessage) {
        checkConstraintViolation(t, Collections.singletonMap(constraintName, constraintMessage));
    }

    public static final void checkConstraintViolation(Exception t, @NotNull String constraintName1, @NotNull String constraintMessage1, @NotNull String constraintName2, @NotNull String constraintMessage2) {
        checkConstraintViolation(t, Map.of(constraintName1, constraintMessage1, constraintName2, constraintMessage2));
    }

    public static final void checkConstraintViolation(Exception t, @NotNull Map<String, String> constraints) {
        @NotNull var exOpt = extractConstraintViolationException(t);
        if (exOpt.isPresent()) {
            @NotNull var ex = exOpt.get();
            if (StringUtils.isNotEmpty(ex.getConstraintName())) {
                var constraintName = ex.getConstraintName();
                for (@NotNull var constraintMessage : constraints.entrySet()) {
                    if (constraintName.equals(constraintMessage.getKey())) {
                        throw new DataValidationException(constraintMessage.getValue());
                    }
                }
            }
        }
    }

    protected void checkAssignedEntityViewsToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(tenantId, entityId);
        if (entityViews != null && !entityViews.isEmpty()) {
            EntityView entityView = entityViews.get(0);
            @NotNull Boolean relationExists = relationService.checkRelation(
                    tenantId, edgeId, entityView.getId(),
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE
                                                                           );
            if (relationExists) {
                throw new DataValidationException("Can't unassign device/asset from edge that is related to entity view and entity view is assigned to edge!");
            }
        }
    }

}
