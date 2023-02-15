package org.thingsboard.server.service.sync.vc.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RepositorySettingsCache")
public class RepositorySettingsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, RepositorySettings> {

    public RepositorySettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.REPOSITORY_SETTINGS_CACHE);
    }

}
