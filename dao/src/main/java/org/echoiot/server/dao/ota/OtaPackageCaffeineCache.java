package org.echoiot.server.dao.ota;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("OtaPackageCache")
public class OtaPackageCaffeineCache extends CaffeineTbTransactionalCache<OtaPackageCacheKey, OtaPackageInfo> {

    public OtaPackageCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.OTA_PACKAGE_CACHE);
    }

}
