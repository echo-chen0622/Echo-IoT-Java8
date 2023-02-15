package org.thingsboard.server.service.install;

public interface SystemDataLoaderService {

    void createSysAdmin() throws Exception;

    void createDefaultTenantProfiles() throws Exception;

    void createAdminSettings() throws Exception;

    void createRandomJwtSettings() throws Exception;

    void saveLegacyYmlSettings() throws Exception;

    void createOAuth2Templates() throws Exception;

    void loadSystemWidgets() throws Exception;

    void updateSystemWidgets() throws Exception;

    void loadDemoData() throws Exception;

    void deleteSystemWidgetBundle(String bundleAlias) throws Exception;

    void createQueues();
}
