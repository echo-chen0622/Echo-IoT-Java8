package org.echoiot.server.cache.usersUpdateTime;

import org.echoiot.server.common.data.CacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CaffeineTbTransactionalCache;


@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("UsersSessionInvalidation")
public class UsersSessionInvalidationCaffeineCache extends CaffeineTbTransactionalCache<String, Long> {

    @Resource
    public UsersSessionInvalidationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.USERS_SESSION_INVALIDATION_CACHE);
    }
}
