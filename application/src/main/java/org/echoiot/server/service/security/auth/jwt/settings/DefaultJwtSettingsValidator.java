package org.echoiot.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.echoiot.server.common.data.security.model.JwtSettings;
import org.echoiot.server.dao.exception.DataValidationException;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DefaultJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    public void validate(JwtSettings jwtSettings) {
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) <= TimeUnit.MINUTES.toSeconds(15)) {
            throw new DataValidationException("JWT refresh token expiration time should be at least 15 minutes!");
        }
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) <= TimeUnit.MINUTES.toSeconds(1)) {
            throw new DataValidationException("JWT token expiration time should be at least 1 minute!");
        }
        if (jwtSettings.getTokenExpirationTime() >= jwtSettings.getRefreshTokenExpTime()) {
            throw new DataValidationException("JWT token expiration time should greater than JWT refresh token expiration time!");
        }
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be a valid Base64 encoded string! " + e.getMessage());
        }

        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        if (decodedKey.length * Byte.SIZE < 256 && !JwtSettingsService.TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be a Base64 encoded string representing at least 256 bits of data!");
        }

        System.arraycopy(decodedKey, 0, RandomUtils.nextBytes(decodedKey.length), 0, decodedKey.length); //secure memory
    }

}
