package org.echoiot.server.dao.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.cache.CaffeineTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DeviceCredentialsCache")
public class DeviceCredentialsCaffeineCache extends CaffeineTbTransactionalCache<String, DeviceCredentials> {

    public DeviceCredentialsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DEVICE_CREDENTIALS_CACHE);
    }

}
