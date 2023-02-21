package org.echoiot.server.service.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.service.security.auth.MfaAuthenticationToken;
import org.echoiot.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component(value = "defaultAuthenticationSuccessHandler")
@RequiredArgsConstructor
public class RestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @NotNull
    private final ObjectMapper mapper;
    @NotNull
    private final JwtTokenFactory tokenFactory;
    @NotNull
    private final TwoFaConfigManager twoFaConfigManager;

    @Override
    public void onAuthenticationSuccess(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                        @NotNull Authentication authentication) throws IOException, ServletException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        JwtPair tokenPair = new JwtPair();

        if (authentication instanceof MfaAuthenticationToken) {
            int preVerificationTokenLifetime = twoFaConfigManager.getPlatformTwoFaSettings(securityUser.getTenantId(), true)
                    .flatMap(settings -> Optional.ofNullable(settings.getTotalAllowedTimeForVerification())
                            .filter(time -> time > 0))
                    .orElse((int) TimeUnit.MINUTES.toSeconds(30));
            tokenPair.setToken(tokenFactory.createPreVerificationToken(securityUser, preVerificationTokenLifetime).getToken());
            tokenPair.setRefreshToken(null);
            tokenPair.setScope(Authority.PRE_VERIFICATION_TOKEN);
        } else {
            tokenPair = tokenFactory.createTokenPair(securityUser);
        }

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), tokenPair);

        clearAuthenticationAttributes(request);
    }

    /**
     * Removes temporary authentication-related data which may have been stored
     * in the session during the authentication process..
     *
     */
    protected final void clearAuthenticationAttributes(@NotNull HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
