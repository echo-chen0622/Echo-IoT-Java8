package org.echoiot.server.service.security.permission;

import java.util.Optional;

public interface Permissions {

    Optional<PermissionChecker> getPermissionChecker(PerResource perResource);

}
