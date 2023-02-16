package org.echoiot.server.service.profile;

import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;

public interface TbAssetProfileCache extends RuleEngineAssetProfileCache {

    void evict(TenantId tenantId, AssetProfileId id);

    void evict(TenantId tenantId, AssetId id);

    AssetProfile find(AssetProfileId assetProfileId);

    AssetProfile findOrCreateAssetProfile(TenantId tenantId, String assetType);
}
