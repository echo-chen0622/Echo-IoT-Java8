package org.thingsboard.server.dao.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("EdgeCache")
public class EdgeRedisCache extends RedisTbTransactionalCache<EdgeCacheKey, Edge> {

    public EdgeRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.EDGE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
