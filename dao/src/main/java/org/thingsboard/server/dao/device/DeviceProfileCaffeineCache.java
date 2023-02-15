package org.thingsboard.server.dao.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DeviceProfileCache")
public class DeviceProfileCaffeineCache extends CaffeineTbTransactionalCache<DeviceProfileCacheKey, DeviceProfile> {

    public DeviceProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DEVICE_PROFILE_CACHE);
    }

}
