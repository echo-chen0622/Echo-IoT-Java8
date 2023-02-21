package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends BaseEntityImportService<AssetId, Asset, EntityExportData<Asset>> {

    @NotNull
    private final AssetService assetService;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull Asset asset, @NotNull IdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.getInternalId(asset.getCustomerId()));
    }

    @NotNull
    @Override
    protected Asset prepare(EntitiesImportCtx ctx, @NotNull Asset asset, Asset old, EntityExportData<Asset> exportData, @NotNull IdProvider idProvider) {
        asset.setAssetProfileId(idProvider.getInternalId(asset.getAssetProfileId()));
        return asset;
    }

    @Override
    protected Asset saveOrUpdate(EntitiesImportCtx ctx, Asset asset, EntityExportData<Asset> exportData, IdProvider idProvider) {
        return assetService.saveAsset(asset);
    }

    @NotNull
    @Override
    protected Asset deepCopy(@NotNull Asset asset) {
        return new Asset(asset);
    }

    @Override
    protected void cleanupForComparison(@NotNull Asset e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
