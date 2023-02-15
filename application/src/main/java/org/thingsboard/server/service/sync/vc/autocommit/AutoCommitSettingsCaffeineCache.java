package org.thingsboard.server.service.sync.vc.autocommit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AutoCommitSettingsCache")
public class AutoCommitSettingsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, AutoCommitSettings> {

    public AutoCommitSettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AUTO_COMMIT_SETTINGS_CACHE);
    }

}
