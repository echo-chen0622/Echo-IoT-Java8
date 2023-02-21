package org.echoiot.server.service.sync.ie.exporting.impl;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetExportService extends BaseEntityExportService<AssetId, Asset, EntityExportData<Asset>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, @NotNull Asset asset, EntityExportData<Asset> exportData) {
        asset.setCustomerId(getExternalIdOrElseInternal(ctx, asset.getCustomerId()));
        asset.setAssetProfileId(getExternalIdOrElseInternal(ctx, asset.getAssetProfileId()));
    }

    @NotNull
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET);
    }

}
