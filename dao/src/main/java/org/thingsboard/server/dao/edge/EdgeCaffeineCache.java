package org.thingsboard.server.dao.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EdgeCache")
public class EdgeCaffeineCache extends CaffeineTbTransactionalCache<EdgeCacheKey, Edge> {

    public EdgeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.EDGE_CACHE);
    }

}
