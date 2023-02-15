package org.thingsboard.server.service.security.auth.jwt.settings;

import org.thingsboard.server.common.data.security.model.JwtSettings;

public interface JwtSettingsValidator {

    void validate(JwtSettings jwtSettings);
}
