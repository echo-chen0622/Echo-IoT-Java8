package org.echoiot.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.security.model.JwtSettings;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * During Install or upgrade the validation is suppressed to keep existing data
 * */

@Primary
@Profile("install")
@Component
@RequiredArgsConstructor
public class InstallJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    public void validate(JwtSettings jwtSettings) {

    }

}
