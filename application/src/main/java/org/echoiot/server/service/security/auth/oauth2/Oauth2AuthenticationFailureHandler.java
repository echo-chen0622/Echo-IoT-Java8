package org.echoiot.server.service.security.auth.oauth2;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@TbCoreComponent
@Component(value = "oauth2AuthenticationFailureHandler")
public class Oauth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final SystemSecurityService systemSecurityService;

    @Autowired
    public Oauth2AuthenticationFailureHandler(final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository,
                                              final SystemSecurityService systemSecurityService) {
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.systemSecurityService = systemSecurityService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response, @NotNull AuthenticationException exception)
            throws IOException, ServletException {
        String baseUrl;
        String errorPrefix;
        @Nullable String callbackUrlScheme = null;
        OAuth2AuthorizationRequest authorizationRequest = httpCookieOAuth2AuthorizationRequestRepository.loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            callbackUrlScheme = authorizationRequest.getAttribute(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME);
        }
        if (!StringUtils.isEmpty(callbackUrlScheme)) {
            baseUrl = callbackUrlScheme + ":";
            errorPrefix = "/?error=";
        } else {
            baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
            errorPrefix = "/login?loginError=";
        }
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, baseUrl + errorPrefix +
                URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
    }
}
