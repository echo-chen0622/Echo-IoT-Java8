package org.echoiot.server.dao.tenant;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.TenantProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TenantProfileCache")
public class TenantProfileCaffeineCache extends CaffeineTbTransactionalCache<TenantProfileCacheKey, TenantProfile> {

    public TenantProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TENANT_PROFILE_CACHE);
    }

}
