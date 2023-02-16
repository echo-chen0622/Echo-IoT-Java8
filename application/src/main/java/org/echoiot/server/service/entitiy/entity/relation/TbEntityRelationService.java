package org.echoiot.server.service.entitiy.entity.relation;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;

public interface TbEntityRelationService {

    void save(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws EchoiotException;

    void delete(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws EchoiotException;

    void deleteRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws EchoiotException;

}
