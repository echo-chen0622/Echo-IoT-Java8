package org.echoiot.server.service.sync.vc.repository;

import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RepositorySettingsCache")
public class RepositorySettingsRedisCache extends RedisTbTransactionalCache<TenantId, RepositorySettings> {

    public RepositorySettingsRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.REPOSITORY_SETTINGS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
