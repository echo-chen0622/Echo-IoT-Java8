package org.thingsboard.server.dao.asset;

import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.UUID;

public interface AssetProfileDao extends Dao<AssetProfile>, ExportableEntityDao<AssetProfileId, AssetProfile> {

    AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId);

    AssetProfile save(TenantId tenantId, AssetProfile assetProfile);

    AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile);

    PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink);

    PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink);

    AssetProfile findDefaultAssetProfile(TenantId tenantId);

    AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId);

    AssetProfile findByName(TenantId tenantId, String profileName);
}
