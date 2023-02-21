package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.entityview.TbEntityViewService;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityViewImportService extends BaseEntityImportService<EntityViewId, EntityView, EntityExportData<EntityView>> {

    @NotNull
    private final EntityViewService entityViewService;

    @Lazy
    @Resource
    private TbEntityViewService tbEntityViewService;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull EntityView entityView, @NotNull IdProvider idProvider) {
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(idProvider.getInternalId(entityView.getCustomerId()));
    }

    @NotNull
    @Override
    protected EntityView prepare(EntitiesImportCtx ctx, @NotNull EntityView entityView, EntityView old, EntityExportData<EntityView> exportData, @NotNull IdProvider idProvider) {
        entityView.setEntityId(idProvider.getInternalId(entityView.getEntityId()));
        return entityView;
    }

    @Override
    protected EntityView saveOrUpdate(EntitiesImportCtx ctx, EntityView entityView, EntityExportData<EntityView> exportData, IdProvider idProvider) {
        return entityViewService.saveEntityView(entityView);
    }

    @Override
    protected void onEntitySaved(@NotNull User user, @NotNull EntityView savedEntityView, @Nullable EntityView oldEntityView) throws EchoiotException {
        tbEntityViewService.updateEntityViewAttributes(user.getTenantId(), savedEntityView, oldEntityView, user);
        super.onEntitySaved(user, savedEntityView, oldEntityView);
        clusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                oldEntityView == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    @NotNull
    @Override
    protected EntityView deepCopy(@NotNull EntityView entityView) {
        return new EntityView(entityView);
    }

    @Override
    protected void cleanupForComparison(@NotNull EntityView e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}
