package org.echoiot.server.service.security.permission;

import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.service.security.model.SecurityUser;

public interface AccessControlService {

    void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    <I extends EntityId, T extends HasTenantId> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

}
