package org.thingsboard.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.security.auth.JwtAuthenticationToken;
import org.thingsboard.server.service.security.auth.TokenOutdatingService;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final TokenOutdatingService tokenOutdatingService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser securityUser = tokenFactory.parseAccessJwtToken(rawAccessToken);

        if (tokenOutdatingService.isOutdated(rawAccessToken, securityUser.getId())) {
            throw new JwtExpiredTokenException("Token is outdated");
        }

        return new JwtAuthenticationToken(securityUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
