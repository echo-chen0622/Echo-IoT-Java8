package org.echoiot.server.service.security.permission;

import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.service.security.model.SecurityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface PermissionChecker<I extends EntityId, T extends HasTenantId> {

    default boolean hasPermission(SecurityUser user, Operation operation) {
        return false;
    }

    default boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
        return false;
    }

    class GenericPermissionChecker<I extends EntityId, T extends HasTenantId> implements PermissionChecker<I,T> {

            private final Set<Operation> allowedOperations;

        public GenericPermissionChecker(Operation... operations) {
            allowedOperations = new HashSet<Operation>(Arrays.asList(operations));
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }
    }

    PermissionChecker denyAllPermissionChecker = new PermissionChecker() {};

    PermissionChecker allowAllPermissionChecker = new PermissionChecker<EntityId, HasTenantId>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            return true;
        }
    };


}
