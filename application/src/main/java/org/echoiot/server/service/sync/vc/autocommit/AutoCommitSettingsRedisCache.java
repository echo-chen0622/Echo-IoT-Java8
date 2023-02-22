package org.echoiot.server.service.sync.vc.autocommit;

import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.AutoCommitSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AutoCommitSettingsCache")
public class AutoCommitSettingsRedisCache extends RedisTbTransactionalCache<TenantId, AutoCommitSettings> {

    public AutoCommitSettingsRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.AUTO_COMMIT_SETTINGS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
