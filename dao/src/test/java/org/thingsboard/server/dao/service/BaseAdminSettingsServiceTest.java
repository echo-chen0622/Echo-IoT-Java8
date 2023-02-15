package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.exception.DataValidationException;

public abstract class BaseAdminSettingsServiceTest extends AbstractServiceTest {

    @Test
    public void testFindAdminSettingsByKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "unknown");
        Assert.assertNull(adminSettings);
    }

    @Test
    public void testFindAdminSettingsById() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        AdminSettings foundAdminSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, adminSettings.getId());
        Assert.assertNotNull(foundAdminSettings);
        Assert.assertEquals(adminSettings, foundAdminSettings);
    }

    @Test
    public void testSaveAdminSettings() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        AdminSettings savedAdminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(savedAdminSettings);
        Assert.assertEquals(adminSettings.getJsonValue(), savedAdminSettings.getJsonValue());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAdminSettingsWithEmptyKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey(null);
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
    }

    @Test(expected = DataValidationException.class)
    public void testChangeAdminSettingsKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey("newKey");
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
    }
}
