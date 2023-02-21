package org.echoiot.server.dao.settings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.id.AdminSettingsId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class AdminSettingsServiceImpl implements AdminSettingsService {

    @Resource
    private AdminSettingsDao adminSettingsDao;

    @Resource
    private DataValidator<AdminSettings> adminSettingsValidator;

    @Override
    public AdminSettings findAdminSettingsById(TenantId tenantId, @NotNull AdminSettingsId adminSettingsId) {
        log.trace("Executing findAdminSettingsById [{}]", adminSettingsId);
        Validator.validateId(adminSettingsId, "Incorrect adminSettingsId " + adminSettingsId);
        return  adminSettingsDao.findById(tenantId, adminSettingsId.getId());
    }

    @Override
    public AdminSettings findAdminSettingsByKey(TenantId tenantId, @NotNull String key) {
        log.trace("Executing findAdminSettingsByKey [{}]", key);
        Validator.validateString(key, "Incorrect key " + key);
        return findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, key);
    }

    @Override
    public AdminSettings findAdminSettingsByTenantIdAndKey(@NotNull TenantId tenantId, String key) {
        return adminSettingsDao.findByTenantIdAndKey(tenantId.getId(), key);
    }

    @Override
    public AdminSettings saveAdminSettings(TenantId tenantId, @NotNull AdminSettings adminSettings) {
        log.trace("Executing saveAdminSettings [{}]", adminSettings);
        adminSettingsValidator.validate(adminSettings, data -> tenantId);
        if (adminSettings.getKey().equals("mail") && !adminSettings.getJsonValue().has("password")) {
            AdminSettings mailSettings = findAdminSettingsByKey(tenantId, "mail");
            if (mailSettings != null) {
                ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
            }
        }
        if (adminSettings.getTenantId() == null) {
            adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
        }
        return adminSettingsDao.save(tenantId, adminSettings);
    }

    @Override
    public boolean deleteAdminSettingsByTenantIdAndKey(@NotNull TenantId tenantId, @NotNull String key) {
        log.trace("Executing deleteAdminSettings, tenantId [{}], key [{}]", tenantId, key);
        Validator.validateString(key, "Incorrect key " + key);
        return adminSettingsDao.removeByTenantIdAndKey(tenantId.getId(), key);
    }

    @Override
    public void deleteAdminSettingsByTenantId(@NotNull TenantId tenantId) {
        adminSettingsDao.removeByTenantId(tenantId.getId());
    }

}
