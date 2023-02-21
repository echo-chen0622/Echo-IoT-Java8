package org.echoiot.server.dao.edge;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.edge.Edge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EdgeCache")
public class EdgeCaffeineCache extends CaffeineTbTransactionalCache<EdgeCacheKey, Edge> {

    public EdgeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.EDGE_CACHE);
    }

}
