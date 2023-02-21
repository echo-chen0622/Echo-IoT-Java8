package org.echoiot.server.dao.attributes;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AttributeCache")
public class AttributeCaffeineCache extends CaffeineTbTransactionalCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ATTRIBUTES_CACHE);
    }

}
