package org.echoiot.server.service.sync.vc;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("VersionControlTaskCache")
public class VersionControlTaskCaffeineCache extends CaffeineTbTransactionalCache<UUID, VersionControlTaskCacheEntry> {

    public VersionControlTaskCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.VERSION_CONTROL_TASK_CACHE);
    }

}
