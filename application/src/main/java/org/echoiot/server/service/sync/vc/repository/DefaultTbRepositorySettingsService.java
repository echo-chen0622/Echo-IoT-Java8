package org.echoiot.server.service.sync.vc.repository;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.RepositoryAuthMethod;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.service.sync.vc.TbAbstractVersionControlSettingsService;

@Service
@TbCoreComponent
public class DefaultTbRepositorySettingsService extends TbAbstractVersionControlSettingsService<RepositorySettings> implements TbRepositorySettingsService {

    public static final String SETTINGS_KEY = "entitiesVersionControl";

    public DefaultTbRepositorySettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, RepositorySettings> cache) {
        super(adminSettingsService, cache, RepositorySettings.class, SETTINGS_KEY);
    }

    @Override
    public RepositorySettings restore(TenantId tenantId, @NotNull RepositorySettings settings) {
        @Nullable RepositorySettings storedSettings = get(tenantId);
        if (storedSettings != null) {
            RepositoryAuthMethod authMethod = settings.getAuthMethod();
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(authMethod) && settings.getPassword() == null) {
                settings.setPassword(storedSettings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(authMethod) && settings.getPrivateKey() == null) {
                settings.setPrivateKey(storedSettings.getPrivateKey());
                if (settings.getPrivateKeyPassword() == null) {
                    settings.setPrivateKeyPassword(storedSettings.getPrivateKeyPassword());
                }
            }
        }
        return settings;
    }

    @Nullable
    @Override
    public RepositorySettings get(TenantId tenantId) {
        RepositorySettings settings = super.get(tenantId);
        if (settings != null) {
            settings = new RepositorySettings(settings);
        }
        return settings;
    }

}
