package org.echoiot.server.service.security.permission;

import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.service.security.model.SecurityUser;
import org.springframework.stereotype.Component;

@Component(value="tenantAdminPermissions")
public class TenantAdminPermissions extends AbstractPermissions {

    public TenantAdminPermissions() {
        super();
        put(PerResource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(PerResource.ALARM, tenantEntityPermissionChecker);
        put(PerResource.ASSET, tenantEntityPermissionChecker);
        put(PerResource.DEVICE, tenantEntityPermissionChecker);
        put(PerResource.CUSTOMER, tenantEntityPermissionChecker);
        put(PerResource.DASHBOARD, tenantEntityPermissionChecker);
        put(PerResource.ENTITY_VIEW, tenantEntityPermissionChecker);
        put(PerResource.TENANT, tenantPermissionChecker);
        put(PerResource.RULE_CHAIN, tenantEntityPermissionChecker);
        put(PerResource.USER, userPermissionChecker);
        put(PerResource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(PerResource.WIDGET_TYPE, widgetsPermissionChecker);
        put(PerResource.DEVICE_PROFILE, tenantEntityPermissionChecker);
        put(PerResource.ASSET_PROFILE, tenantEntityPermissionChecker);
        put(PerResource.API_USAGE_STATE, tenantEntityPermissionChecker);
        put(PerResource.TB_RESOURCE, tbResourcePermissionChecker);
        put(PerResource.OTA_PACKAGE, tenantEntityPermissionChecker);
        put(PerResource.EDGE, tenantEntityPermissionChecker);
        put(PerResource.RPC, tenantEntityPermissionChecker);
        put(PerResource.QUEUE, queuePermissionChecker);
        put(PerResource.VERSION_CONTROL, PermissionChecker.allowAllPermissionChecker);
    }

    public static final PermissionChecker tenantEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            return user.getTenantId().equals(entity.getTenantId());
        }
    };

    private static final PermissionChecker tenantPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    if (!super.hasPermission(user, operation, entityId, entity)) {
                        return false;
                    }
                    return user.getTenantId().equals(entityId);
                }

            };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            if (Authority.SYS_ADMIN.equals(userEntity.getAuthority())) {
                return false;
            }
            return user.getTenantId().equals(userEntity.getTenantId());
        }

    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }

    };

    private static final PermissionChecker tbResourcePermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }

    };

    private static final PermissionChecker queuePermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }

    };

}
