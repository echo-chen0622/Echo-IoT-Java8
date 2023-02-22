package org.echoiot.server.service.sync.ie.exporting.impl;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@TbCoreComponent
public class EntityViewExportService extends BaseEntityExportService<EntityViewId, EntityView, EntityExportData<EntityView>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, EntityView entityView, EntityExportData<EntityView> exportData) {
        entityView.setEntityId(getExternalIdOrElseInternal(ctx, entityView.getEntityId()));
        entityView.setCustomerId(getExternalIdOrElseInternal(ctx, entityView.getCustomerId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ENTITY_VIEW);
    }

}
