package org.thingsboard.server.dao.entityview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EntityViewCache")
public class EntityViewCaffeineCache extends CaffeineTbTransactionalCache<EntityViewCacheKey, EntityViewCacheValue> {

    public EntityViewCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ENTITY_VIEW_CACHE);
    }

}
