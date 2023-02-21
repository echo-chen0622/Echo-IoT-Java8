package org.echoiot.server.service.security.auth.oauth2;

import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.controller.AbstractControllerTest;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.annotation.Resource;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class Oauth2AuthenticationSuccessHandlerTest extends AbstractControllerTest {

    @Resource
    private Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Mock
    private JwtTokenFactory jwtTokenFactory;

    private SecurityUser securityUser;

    @Before
    public void before() {
        @NotNull UserId userId = new UserId(UUID.randomUUID());
        securityUser = new SecurityUser(userId);
        when(jwtTokenFactory.createTokenPair(eq(securityUser))).thenReturn(new JwtPair("testAccessToken", "testRefreshToken"));
    }

    @Test
    public void testGetRedirectUrl() {
        @NotNull JwtPair jwtPair = jwtTokenFactory.createTokenPair(securityUser);

        @NotNull String urlWithoutParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e";
        @NotNull String urlWithParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1";

        @NotNull String redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithoutParams, jwtPair);
        @NotNull String expectedUrl = urlWithoutParams + "/?accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);

        redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithParams, jwtPair);
        expectedUrl = urlWithParams + "&accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);
    }
}
