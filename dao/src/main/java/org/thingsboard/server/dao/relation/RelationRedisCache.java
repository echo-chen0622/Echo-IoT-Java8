package org.thingsboard.server.dao.relation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbFSTRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.cache.RedisTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RelationCache")
public class RelationRedisCache extends RedisTbTransactionalCache<RelationCacheKey, RelationCacheValue> {

    public RelationRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.RELATIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
