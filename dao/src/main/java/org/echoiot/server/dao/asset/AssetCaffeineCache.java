package org.echoiot.server.dao.asset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AssetCache")
public class AssetCaffeineCache extends CaffeineTbTransactionalCache<AssetCacheKey, Asset> {

    public AssetCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ASSET_CACHE);
    }

}
