package org.echoiot.server.service.security.permission;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

public abstract class AbstractPermissions extends HashMap<Resource, PermissionChecker> implements Permissions {

    public AbstractPermissions() {
        super();
    }

    @NotNull
    @Override
    public Optional<PermissionChecker> getPermissionChecker(Resource resource) {
        PermissionChecker permissionChecker = this.get(resource);
        return Optional.ofNullable(permissionChecker);
    }
}
