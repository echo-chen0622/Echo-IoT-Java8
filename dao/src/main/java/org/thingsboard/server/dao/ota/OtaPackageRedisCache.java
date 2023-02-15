package org.thingsboard.server.dao.ota;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbFSTRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.cache.RedisTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("OtaPackageCache")
public class OtaPackageRedisCache extends RedisTbTransactionalCache<OtaPackageCacheKey, OtaPackageInfo> {

    public OtaPackageRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.OTA_PACKAGE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
