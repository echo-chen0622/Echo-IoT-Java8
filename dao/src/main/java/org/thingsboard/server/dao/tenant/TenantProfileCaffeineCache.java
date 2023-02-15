package org.thingsboard.server.dao.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TenantProfileCache")
public class TenantProfileCaffeineCache extends CaffeineTbTransactionalCache<TenantProfileCacheKey, TenantProfile> {

    public TenantProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TENANT_PROFILE_CACHE);
    }

}
