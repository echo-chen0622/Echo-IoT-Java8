package org.echoiot.server.service.security.permission;

import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum PerResource {
    ADMIN_SETTINGS(),
    ALARM(EntityType.ALARM),
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CONFIGURATION_INFO(),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    API_USAGE_STATE(EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    EDGE(EntityType.EDGE),
    RPC(EntityType.RPC),
    QUEUE(EntityType.QUEUE),
    VERSION_CONTROL;

    @Nullable
    private final EntityType entityType;

    PerResource() {
        this.entityType = null;
    }

    PerResource(EntityType entityType) {
        this.entityType = entityType;
    }

    @NotNull
    public Optional<EntityType> getEntityType() {
        return Optional.ofNullable(entityType);
    }

    @NotNull
    public static PerResource of(@NotNull EntityType entityType) {
        for (@NotNull PerResource perResource : PerResource.values()) {
            if (perResource.getEntityType().orElse(null) == entityType) {
                return perResource;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType: " + entityType.name());
    }
}
