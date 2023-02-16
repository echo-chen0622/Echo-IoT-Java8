package org.echoiot.server.dao.asset;

import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetProfileInfo;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

public interface AssetProfileService {

    AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId);

    AssetProfile findAssetProfileByName(TenantId tenantId, String profileName);

    AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, AssetProfileId assetProfileId);

    AssetProfile saveAssetProfile(AssetProfile assetProfile);

    void deleteAssetProfile(TenantId tenantId, AssetProfileId assetProfileId);

    PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink);

    PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink);

    AssetProfile findOrCreateAssetProfile(TenantId tenantId, String profileName);

    AssetProfile createDefaultAssetProfile(TenantId tenantId);

    AssetProfile findDefaultAssetProfile(TenantId tenantId);

    AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId);

    boolean setDefaultAssetProfile(TenantId tenantId, AssetProfileId assetProfileId);

    void deleteAssetProfilesByTenantId(TenantId tenantId);

}
