package org.thingsboard.server.dao.asset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AssetCache")
public class AssetCaffeineCache extends CaffeineTbTransactionalCache<AssetCacheKey, Asset> {

    public AssetCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ASSET_CACHE);
    }

}
