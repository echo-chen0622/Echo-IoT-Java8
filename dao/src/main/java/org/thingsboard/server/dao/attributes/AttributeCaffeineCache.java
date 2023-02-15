package org.thingsboard.server.dao.attributes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AttributeCache")
public class AttributeCaffeineCache extends CaffeineTbTransactionalCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ATTRIBUTES_CACHE);
    }

}
