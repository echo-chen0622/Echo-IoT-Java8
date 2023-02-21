package org.echoiot.server.service.sync.vc.repository;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RepositorySettingsCache")
public class RepositorySettingsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, RepositorySettings> {

    public RepositorySettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.REPOSITORY_SETTINGS_CACHE);
    }

}
