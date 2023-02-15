package org.thingsboard.server.cache.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Device;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DeviceCache")
public class DeviceCaffeineCache extends CaffeineTbTransactionalCache<DeviceCacheKey, Device> {

    public DeviceCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DEVICE_CACHE);
    }

}
