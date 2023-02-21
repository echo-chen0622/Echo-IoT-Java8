package org.echoiot.server.service.session;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.gen.transport.TransportProtos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("SessionCache")
public class SessionCaffeineCache extends CaffeineTbTransactionalCache<DeviceId, TransportProtos.DeviceSessionsCacheEntry> {

    public SessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.SESSIONS_CACHE);
    }

}
