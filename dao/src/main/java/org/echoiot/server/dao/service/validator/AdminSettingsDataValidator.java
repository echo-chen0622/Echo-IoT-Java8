package org.echoiot.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AdminSettingsDataValidator extends DataValidator<AdminSettings> {

    @NotNull
    private final AdminSettingsService adminSettingsService;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull AdminSettings adminSettings) {
        AdminSettings existentAdminSettingsWithKey = adminSettingsService.findAdminSettingsByKey(tenantId, adminSettings.getKey());
        if (existentAdminSettingsWithKey != null) {
            throw new DataValidationException("Admin settings with such name already exists!");
        }
    }

    @Nullable
    @Override
    protected AdminSettings validateUpdate(TenantId tenantId, @NotNull AdminSettings adminSettings) {
        AdminSettings existentAdminSettings = adminSettingsService.findAdminSettingsById(tenantId, adminSettings.getId());
        if (existentAdminSettings != null) {
            if (!existentAdminSettings.getKey().equals(adminSettings.getKey())) {
                throw new DataValidationException("Changing key of admin settings entry is prohibited!");
            }
        }
        return existentAdminSettings;
    }


    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull AdminSettings adminSettings) {
        if (StringUtils.isEmpty(adminSettings.getKey())) {
            throw new DataValidationException("Key should be specified!");
        }
        if (adminSettings.getJsonValue() == null) {
            throw new DataValidationException("Json value should be specified!");
        }
    }
}
