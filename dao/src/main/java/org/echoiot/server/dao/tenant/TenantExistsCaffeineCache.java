package org.echoiot.server.dao.tenant;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TenantExistsCache")
public class TenantExistsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, Boolean> {

    public TenantExistsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TENANTS_EXIST_CACHE);
    }

}
