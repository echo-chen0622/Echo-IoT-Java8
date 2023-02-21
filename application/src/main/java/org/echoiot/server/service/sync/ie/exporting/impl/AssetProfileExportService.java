package org.echoiot.server.service.sync.ie.exporting.impl;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetProfileExportService extends BaseEntityExportService<AssetProfileId, AssetProfile, EntityExportData<AssetProfile>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, @NotNull AssetProfile assetProfile, EntityExportData<AssetProfile> exportData) {
        assetProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultRuleChainId()));
    }

    @NotNull
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET_PROFILE);
    }

}
