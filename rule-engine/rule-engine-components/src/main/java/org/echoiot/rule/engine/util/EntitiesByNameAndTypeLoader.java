package org.echoiot.rule.engine.util;

import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntitiesByNameAndTypeLoader {

    private static final List<EntityType> AVAILABLE_ENTITY_TYPES = List.of(
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.ENTITY_VIEW,
            EntityType.EDGE,
            EntityType.USER);

    public static EntityId findEntityId(@NotNull TbContext ctx, @NotNull EntityType entityType, String entityName) {
        SearchTextBasedWithAdditionalInfo<? extends EntityId> targetEntity;
        switch (entityType) {
            case DEVICE:
                targetEntity = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ASSET:
                targetEntity = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ENTITY_VIEW:
                targetEntity = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case EDGE:
                targetEntity = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case USER:
                targetEntity = ctx.getUserService().findUserByTenantIdAndEmail(ctx.getTenantId(), entityName);
                break;
            default:
                throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
        if (targetEntity == null) {
            throw new IllegalStateException("Failed to found " + entityType.name() + "  entity by name: '" + entityName + "'!");
        }
        return targetEntity.getId();
    }

    public static void checkEntityType(@NotNull EntityType entityType) {
        if (!AVAILABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
    }

}
