package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends BaseEntityImportService<AssetId, Asset, EntityExportData<Asset>> {

    private final AssetService assetService;

    @Override
    protected void setOwner(TenantId tenantId, Asset asset, IdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.getInternalId(asset.getCustomerId()));
    }

    @Override
    protected Asset prepare(EntitiesImportCtx ctx, Asset asset, Asset old, EntityExportData<Asset> exportData, IdProvider idProvider) {
        asset.setAssetProfileId(idProvider.getInternalId(asset.getAssetProfileId()));
        return asset;
    }

    @Override
    protected Asset saveOrUpdate(EntitiesImportCtx ctx, Asset asset, EntityExportData<Asset> exportData, IdProvider idProvider) {
        return assetService.saveAsset(asset);
    }

    @Override
    protected Asset deepCopy(Asset asset) {
        return new Asset(asset);
    }

    @Override
    protected void cleanupForComparison(Asset e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
