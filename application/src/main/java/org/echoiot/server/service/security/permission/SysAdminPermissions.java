package org.echoiot.server.service.security.permission;

import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component(value="sysAdminPermissions")
public class SysAdminPermissions extends AbstractPermissions {

    public SysAdminPermissions() {
        super();
        put(PerResource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.DASHBOARD, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(PerResource.TENANT, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.RULE_CHAIN, systemEntityPermissionChecker);
        put(PerResource.USER, userPermissionChecker);
        put(PerResource.WIDGETS_BUNDLE, systemEntityPermissionChecker);
        put(PerResource.WIDGET_TYPE, systemEntityPermissionChecker);
        put(PerResource.OAUTH2_CONFIGURATION_INFO, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.OAUTH2_CONFIGURATION_TEMPLATE, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.TENANT_PROFILE, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.TB_RESOURCE, systemEntityPermissionChecker);
        put(PerResource.QUEUE, systemEntityPermissionChecker);
    }

    private static final PermissionChecker systemEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, @NotNull HasTenantId entity) {

            return entity.getTenantId() == null || entity.getTenantId().isNullUid();
        }
    };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, @NotNull User userEntity) {
            return !Authority.CUSTOMER_USER.equals(userEntity.getAuthority());
        }

    };

}
