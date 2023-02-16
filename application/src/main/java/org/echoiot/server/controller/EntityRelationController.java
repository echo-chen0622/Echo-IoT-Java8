package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntityRelationInfo;
import org.echoiot.server.common.data.relation.EntityRelationsQuery;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class EntityRelationController extends BaseController {

    private final TbEntityRelationService tbEntityRelationService;

    public static final String TO_TYPE = "toType";
    public static final String FROM_ID = "fromId";
    public static final String FROM_TYPE = "fromType";
    public static final String RELATION_TYPE = "relationType";
    public static final String TO_ID = "toId";

    private static final String SECURITY_CHECKS_ENTITIES_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that 'from' and 'to' entities are owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that 'from' and 'to' entities are owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the 'from' and 'to' entities are assigned to the same customer.";

    private static final String SECURITY_CHECKS_ENTITY_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that the entity is owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the entity is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the entity is assigned to the same customer.";

    @ApiOperation(value = "Create Relation (saveRelation)",
            notes = "Creates or updates a relation between two entities in the platform. " +
                    "Relations unique key is a combination of from/to entity id and relation type group and relation type. " +
                    SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveRelation(@ApiParam(value = "A JSON value representing the relation.", required = true)
                             @RequestBody EntityRelation relation) throws EchoiotException {
        checkNotNull(relation);
        checkCanCreateRelation(relation.getFrom());
        checkCanCreateRelation(relation.getTo());
        if (relation.getTypeGroup() == null) {
            relation.setTypeGroup(RelationTypeGroup.COMMON);
        }

        tbEntityRelationService.save(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete Relation (deleteRelation)",
            notes = "Deletes a relation between two entities in the platform. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.DELETE, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelation(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                               @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                               @ApiParam(value = ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                               @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                               @ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                               @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws EchoiotException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkCanCreateRelation(fromId);
        checkCanCreateRelation(toId);

        RelationTypeGroup relationTypeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        EntityRelation relation = new EntityRelation(fromId, toId, strRelationType, relationTypeGroup);
        tbEntityRelationService.delete(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete Relations (deleteRelations)",
            notes = "Deletes all the relation (both 'from' and 'to' direction) for the specified entity. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.DELETE, params = {"entityId", "entityType"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelations(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam("entityId") String strId,
                                @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam("entityType") String strType) throws EchoiotException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId, Operation.WRITE);
        tbEntityRelationService.deleteRelations(getTenantId(), getCurrentUser().getCustomerId(), entityId, getCurrentUser());
    }

    @ApiOperation(value = "Get Relation (getRelation)",
            notes = "Returns relation object between two specified entities if present. Otherwise throws exception. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseBody
    public EntityRelation getRelation(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                      @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                      @ApiParam(value = ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                      @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                                      @ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                      @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws EchoiotException {
        try {
            checkParameter(FROM_ID, strFromId);
            checkParameter(FROM_TYPE, strFromType);
            checkParameter(RELATION_TYPE, strRelationType);
            checkParameter(TO_ID, strToId);
            checkParameter(TO_TYPE, strToType);
            EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
            EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
            checkEntityId(fromId, Operation.READ);
            checkEntityId(toId, Operation.READ);
            RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
            return checkNotNull(relationService.getRelation(getTenantId(), fromId, toId, strRelationType, typeGroup));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relations (findByFrom)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    public List<EntityRelation> findByFrom(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                           @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                           @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByFrom(getTenantId(), entityId, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByFrom)",
            notes = "Returns list of relation info objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + ControllerConstants.RELATION_INFO_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByFrom(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                                   @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                                   @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByFrom(getTenantId(), entityId, typeGroup).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relations (findByFrom)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE})
    @ResponseBody
    public List<EntityRelation> findByFrom(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                           @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                           @ApiParam(value = ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                           @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByFromAndType(getTenantId(), entityId, strRelationType, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relations (findByTo)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    public List<EntityRelation> findByTo(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByTo(getTenantId(), entityId, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByTo)",
            notes = "Returns list of relation info objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + ControllerConstants.RELATION_INFO_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByTo(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                                 @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                                 @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                 @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByTo(getTenantId(), entityId, typeGroup).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get List of Relations (findByTo)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE, RELATION_TYPE})
    @ResponseBody
    public List<EntityRelation> findByTo(@ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @ApiParam(value = ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                         @ApiParam(value = ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws EchoiotException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByToAndType(getTenantId(), entityId, strRelationType, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find related entities (findByQuery)",
            notes = "Returns all entities that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelation> findByQuery(@ApiParam(value = "A JSON value representing the entity relations query object.", required = true)
                                            @RequestBody EntityRelationsQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByQuery(getTenantId(), query).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find related entity infos (findInfoByQuery)",
            notes = "Returns all entity infos that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info. " + ControllerConstants.RELATION_INFO_DESCRIPTION, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelationInfo> findInfoByQuery(@ApiParam(value = "A JSON value representing the entity relations query object.", required = true)
                                                    @RequestBody EntityRelationsQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByQuery(getTenantId(), query).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkCanCreateRelation(EntityId entityId) throws EchoiotException {
        SecurityUser currentUser = getCurrentUser();
        var isTenantAdminAndRelateToSelf = currentUser.isTenantAdmin() && currentUser.getTenantId().equals(entityId);
        if (!isTenantAdminAndRelateToSelf) {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    private <T extends EntityRelation> List<T> filterRelationsByReadPermission(List<T> relationsByQuery) {
        return relationsByQuery.stream().filter(relationByQuery -> {
            try {
                checkEntityId(relationByQuery.getTo(), Operation.READ);
            } catch (EchoiotException e) {
                return false;
            }
            try {
                checkEntityId(relationByQuery.getFrom(), Operation.READ);
            } catch (EchoiotException e) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup, RelationTypeGroup defaultValue) {
        RelationTypeGroup result = defaultValue;
        if (strRelationTypeGroup != null && strRelationTypeGroup.trim().length() > 0) {
            try {
                result = RelationTypeGroup.valueOf(strRelationTypeGroup);
            } catch (IllegalArgumentException e) {
            }
        }
        return result;
    }

}
