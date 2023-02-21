package org.echoiot.server.dao.asset;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AssetProfileCache")
public class AssetProfileCaffeineCache extends CaffeineTbTransactionalCache<AssetProfileCacheKey, AssetProfile> {

    public AssetProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ASSET_PROFILE_CACHE);
    }

}
