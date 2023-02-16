package org.echoiot.server.service.security.auth.jwt.settings;

import org.echoiot.server.common.data.security.model.JwtSettings;

public interface JwtSettingsService {

    String ADMIN_SETTINGS_JWT_KEY = "jwt";
    String TOKEN_SIGNING_KEY_DEFAULT = "echoiotDefaultSigningKey";

    JwtSettings getJwtSettings();

    JwtSettings reloadJwtSettings();

    void createRandomJwtSettings();

    void saveLegacyYmlSettings();

    JwtSettings saveJwtSettings(JwtSettings jwtSettings);

}
