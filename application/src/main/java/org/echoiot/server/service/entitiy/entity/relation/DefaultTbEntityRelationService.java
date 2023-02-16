package org.echoiot.server.service.entitiy.entity.relation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Service;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityRelationService extends AbstractTbEntityService implements TbEntityRelationService {

    private final RelationService relationService;

    @Override
    public void save(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws EchoiotException {
        try {
            relationService.saveRelation(tenantId, relation);
            notificationEntityService.notifyRelation(tenantId, customerId,
                                                     relation, user, ActionType.RELATION_ADD_OR_UPDATE, relation);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, relation.getFrom(), null, customerId,
                    ActionType.RELATION_ADD_OR_UPDATE, user, e, relation);
            notificationEntityService.logEntityAction(tenantId, relation.getTo(), null, customerId,
                    ActionType.RELATION_ADD_OR_UPDATE, user, e, relation);
            throw e;
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws EchoiotException {
        try {
            boolean found = relationService.deleteRelation(tenantId, relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
            if (!found) {
                throw new EchoiotException("Requested item wasn't found!", EchoiotErrorCode.ITEM_NOT_FOUND);
            }
            notificationEntityService.notifyRelation(tenantId, customerId,
                    relation, user, ActionType.RELATION_DELETED, relation);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, relation.getFrom(), null, customerId,
                    ActionType.RELATION_DELETED, user, e, relation);
            notificationEntityService.logEntityAction(tenantId, relation.getTo(), null, customerId,
                    ActionType.RELATION_DELETED, user, e, relation);
            throw e;
        }
    }

    @Override
    public void deleteRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws EchoiotException {
        try {
            relationService.deleteEntityRelations(tenantId, entityId);
            notificationEntityService.logEntityAction(tenantId, entityId, null, customerId, ActionType.RELATIONS_DELETED, user);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, entityId, null, customerId,
                    ActionType.RELATIONS_DELETED, user, e);
            throw e;
        }
    }
}