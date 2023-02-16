package org.echoiot.server.dao.settings;

import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.id.AdminSettingsId;
import org.echoiot.server.common.data.id.TenantId;

public interface AdminSettingsService {

    AdminSettings findAdminSettingsById(TenantId tenantId, AdminSettingsId adminSettingsId);

    AdminSettings findAdminSettingsByKey(TenantId tenantId, String key);

    AdminSettings findAdminSettingsByTenantIdAndKey(TenantId tenantId, String key);

    AdminSettings saveAdminSettings(TenantId tenantId, AdminSettings adminSettings);

    boolean deleteAdminSettingsByTenantIdAndKey(TenantId tenantId, String key);

    void deleteAdminSettingsByTenantId(TenantId tenantId);

}
