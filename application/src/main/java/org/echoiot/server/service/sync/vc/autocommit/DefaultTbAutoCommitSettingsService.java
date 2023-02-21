package org.echoiot.server.service.sync.vc.autocommit;

import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.AutoCommitSettings;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.TbAbstractVersionControlSettingsService;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
public class DefaultTbAutoCommitSettingsService extends TbAbstractVersionControlSettingsService<AutoCommitSettings> implements TbAutoCommitSettingsService {

    public static final String SETTINGS_KEY = "autoCommitSettings";

    public DefaultTbAutoCommitSettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, AutoCommitSettings> cache) {
        super(adminSettingsService, cache, AutoCommitSettings.class, SETTINGS_KEY);
    }

}
