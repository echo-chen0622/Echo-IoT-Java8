package org.thingsboard.server.service.security.auth.jwt.settings;

import org.thingsboard.server.common.data.security.model.JwtSettings;

public interface JwtSettingsService {

    String ADMIN_SETTINGS_JWT_KEY = "jwt";
    String TOKEN_SIGNING_KEY_DEFAULT = "thingsboardDefaultSigningKey";

    JwtSettings getJwtSettings();

    JwtSettings reloadJwtSettings();

    void createRandomJwtSettings();

    void saveLegacyYmlSettings();

    JwtSettings saveJwtSettings(JwtSettings jwtSettings);

}
