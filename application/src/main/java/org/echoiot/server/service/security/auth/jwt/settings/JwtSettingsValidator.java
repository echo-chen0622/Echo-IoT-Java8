package org.echoiot.server.service.security.auth.jwt.settings;

import org.echoiot.server.common.data.security.model.JwtSettings;

public interface JwtSettingsValidator {

    void validate(JwtSettings jwtSettings);
}
