package org.echoiot.server.service.entitiy.entity.relation;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;

public interface TbEntityRelationService {

    void save(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws ThingsboardException;

    void delete(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws ThingsboardException;

    void deleteRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws ThingsboardException;

}
