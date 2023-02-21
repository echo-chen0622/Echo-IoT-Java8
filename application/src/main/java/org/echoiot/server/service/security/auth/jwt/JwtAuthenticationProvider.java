package org.echoiot.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.service.security.auth.JwtAuthenticationToken;
import org.echoiot.server.service.security.auth.TokenOutdatingService;
import org.echoiot.server.service.security.exception.JwtExpiredTokenException;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @NotNull
    private final JwtTokenFactory tokenFactory;
    @NotNull
    private final TokenOutdatingService tokenOutdatingService;

    @NotNull
    @Override
    public Authentication authenticate(@NotNull Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser securityUser = tokenFactory.parseAccessJwtToken(rawAccessToken);

        if (tokenOutdatingService.isOutdated(rawAccessToken, securityUser.getId())) {
            throw new JwtExpiredTokenException("Token is outdated");
        }

        return new JwtAuthenticationToken(securityUser);
    }

    @Override
    public boolean supports(@NotNull Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
