package org.echoiot.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DefaultAccessControlService implements AccessControlService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";

    private final Map<Authority, Permissions> authorityPermissions = new HashMap<>();

    public DefaultAccessControlService(
            @Qualifier("sysAdminPermissions") Permissions sysAdminPermissions,
            @Qualifier("tenantAdminPermissions") Permissions tenantAdminPermissions,
            @Qualifier("customerUserPermissions") Permissions customerUserPermissions) {
        authorityPermissions.put(Authority.SYS_ADMIN, sysAdminPermissions);
        authorityPermissions.put(Authority.TENANT_ADMIN, tenantAdminPermissions);
        authorityPermissions.put(Authority.CUSTOMER_USER, customerUserPermissions);
    }

    @Override
    public void checkPermission(@NotNull SecurityUser user, PerResource perResource, Operation operation) throws EchoiotException {
        @NotNull PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), perResource);
        if (!permissionChecker.hasPermission(user, operation)) {
            permissionDenied();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends EntityId, T extends HasTenantId> void checkPermission(@NotNull SecurityUser user, PerResource perResource,
                                                                            Operation operation, I entityId, T entity) throws EchoiotException {
        @NotNull PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), perResource);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity)) {
            permissionDenied();
        }
    }

    @NotNull
    private PermissionChecker getPermissionChecker(Authority authority, PerResource perResource) throws EchoiotException {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions == null) {
            permissionDenied();
        }
        Optional<PermissionChecker> permissionChecker = permissions.getPermissionChecker(perResource);
        if (!permissionChecker.isPresent()) {
            permissionDenied();
        }
        return permissionChecker.get();
    }

    private void permissionDenied() throws EchoiotException {
        throw new EchoiotException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                                       EchoiotErrorCode.PERMISSION_DENIED);
    }

}
