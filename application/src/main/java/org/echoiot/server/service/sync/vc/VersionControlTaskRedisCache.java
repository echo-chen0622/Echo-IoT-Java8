package org.echoiot.server.service.sync.vc;

import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;
import org.echoiot.server.common.data.CacheConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("VersionControlTaskCache")
public class VersionControlTaskRedisCache extends RedisTbTransactionalCache<UUID, VersionControlTaskCacheEntry> {

    public VersionControlTaskRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.VERSION_CONTROL_TASK_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
