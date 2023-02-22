package org.echoiot.server.service.security.permission;


import java.util.HashMap;
import java.util.Optional;

public abstract class AbstractPermissions extends HashMap<PerResource, PermissionChecker> implements Permissions {

    public AbstractPermissions() {
        super();
    }

    @Override
    public Optional<PermissionChecker> getPermissionChecker(PerResource perResource) {
        PermissionChecker permissionChecker = this.get(perResource);
        return Optional.ofNullable(permissionChecker);
    }
}
