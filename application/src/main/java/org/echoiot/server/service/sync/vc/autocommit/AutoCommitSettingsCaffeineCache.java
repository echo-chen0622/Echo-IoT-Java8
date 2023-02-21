package org.echoiot.server.service.sync.vc.autocommit;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.AutoCommitSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AutoCommitSettingsCache")
public class AutoCommitSettingsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, AutoCommitSettings> {

    public AutoCommitSettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AUTO_COMMIT_SETTINGS_CACHE);
    }

}
