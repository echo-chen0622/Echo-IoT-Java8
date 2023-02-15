package org.thingsboard.server.dao.settings;

import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface AdminSettingsDao extends Dao<AdminSettings> {

    /**
     * Save or update admin settings object
     *
     * @param adminSettings the admin settings object
     * @return saved admin settings object
     */
    AdminSettings save(TenantId tenantId, AdminSettings adminSettings);

    /**
     * Find admin settings by key.
     *
     * @param key the key
     * @return the admin settings object
     */
    AdminSettings findByTenantIdAndKey(UUID tenantId, String key);

    boolean removeByTenantIdAndKey(UUID tenantId, String key);

    void removeByTenantId(UUID tenantId);

}
