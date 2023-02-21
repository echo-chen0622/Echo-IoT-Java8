package org.echoiot.server.dao.entityview;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EntityViewCache")
public class EntityViewCaffeineCache extends CaffeineTbTransactionalCache<EntityViewCacheKey, EntityViewCacheValue> {

    public EntityViewCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ENTITY_VIEW_CACHE);
    }

}
