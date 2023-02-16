package org.echoiot.server.dao.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TenantCache")
public class TenantCaffeineCache extends CaffeineTbTransactionalCache<TenantId, Tenant> {

    public TenantCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TENANTS_CACHE);
    }

}
