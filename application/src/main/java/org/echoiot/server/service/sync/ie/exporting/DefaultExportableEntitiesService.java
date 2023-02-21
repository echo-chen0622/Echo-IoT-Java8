package org.echoiot.server.service.sync.ie.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.ExportableEntityDao;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.HasId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.service.security.permission.AccessControlService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();
    private final Map<EntityType, BiConsumer<TenantId, EntityId>> removers = new HashMap<>();

    @NotNull
    private final AccessControlService accessControlService;


    @Nullable
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(@NotNull TenantId tenantId, @NotNull I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        @Nullable E entity = null;

        if (dao instanceof ExportableEntityDao) {
            @NotNull ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            entity = exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Nullable
    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(@NotNull TenantId tenantId, @NotNull I id) {
        E entity = findEntityById(id);

        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }
        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityById(@NotNull I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }

        return dao.findById(TenantId.SYS_TENANT_ID, id.getId());
    }

    @Nullable
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(@NotNull TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        @Nullable E entity = null;

        if (dao instanceof ExportableEntityDao) {
            @NotNull ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            try {
                entity = exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> PageData<E> findEntitiesByTenantId(@NotNull TenantId tenantId, EntityType entityType, PageLink pageLink) {
        @Nullable ExportableEntityDao<I, E> dao = getExportableEntityDao(entityType);
        if (dao != null) {
            return dao.findByTenantId(tenantId.getId(), pageLink);
        } else {
            return new PageData<>();
        }
    }

    @Nullable
    @Override
    public <I extends EntityId> I getExternalIdByInternal(@NotNull I internalId) {
        @Nullable ExportableEntityDao<I, ?> dao = getExportableEntityDao(internalId.getEntityType());
        if (dao != null) {
            return dao.getExternalIdByInternal(internalId);
        } else {
            return null;
        }
    }

    private boolean belongsToTenant(@NotNull HasId<? extends EntityId> entity, @NotNull TenantId tenantId) {
        return tenantId.equals(((HasTenantId) entity).getTenantId());
    }


    @Override
    public <I extends EntityId> void removeById(TenantId tenantId, @NotNull I id) {
        EntityType entityType = id.getEntityType();
        BiConsumer<TenantId, EntityId> entityRemover = removers.get(entityType);
        if (entityRemover == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }
        entityRemover.accept(tenantId, id);
    }

    @Nullable
    private <I extends EntityId, E extends ExportableEntity<I>> ExportableEntityDao<I, E> getExportableEntityDao(EntityType entityType) {
        Dao<E> dao = getDao(entityType);
        if (dao instanceof ExportableEntityDao) {
            return (ExportableEntityDao<I, E>) dao;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    @Resource
    private void setDaos(@NotNull Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

    @Resource
    private void setRemovers(@NotNull CustomerService customerService, @NotNull AssetService assetService, @NotNull RuleChainService ruleChainService,
                             @NotNull DashboardService dashboardService, @NotNull DeviceProfileService deviceProfileService,
                             @NotNull AssetProfileService assetProfileService, @NotNull DeviceService deviceService, @NotNull WidgetsBundleService widgetsBundleService) {
        removers.put(EntityType.CUSTOMER, (tenantId, entityId) -> {
            customerService.deleteCustomer(tenantId, (CustomerId) entityId);
        });
        removers.put(EntityType.ASSET, (tenantId, entityId) -> {
            assetService.deleteAsset(tenantId, (AssetId) entityId);
        });
        removers.put(EntityType.RULE_CHAIN, (tenantId, entityId) -> {
            ruleChainService.deleteRuleChainById(tenantId, (RuleChainId) entityId);
        });
        removers.put(EntityType.DASHBOARD, (tenantId, entityId) -> {
            dashboardService.deleteDashboard(tenantId, (DashboardId) entityId);
        });
        removers.put(EntityType.DEVICE_PROFILE, (tenantId, entityId) -> {
            deviceProfileService.deleteDeviceProfile(tenantId, (DeviceProfileId) entityId);
        });
        removers.put(EntityType.ASSET_PROFILE, (tenantId, entityId) -> {
            assetProfileService.deleteAssetProfile(tenantId, (AssetProfileId) entityId);
        });
        removers.put(EntityType.DEVICE, (tenantId, entityId) -> {
            deviceService.deleteDevice(tenantId, (DeviceId) entityId);
        });
        removers.put(EntityType.WIDGETS_BUNDLE, (tenantId, entityId) -> {
            widgetsBundleService.deleteWidgetsBundle(tenantId, (WidgetsBundleId) entityId);
        });
    }

}
