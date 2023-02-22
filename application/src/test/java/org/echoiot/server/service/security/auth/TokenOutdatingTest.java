package org.echoiot.server.service.security.auth;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.echoiot.server.common.data.security.event.UserSessionInvalidationEvent;
import org.echoiot.server.common.data.security.model.JwtToken;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.echoiot.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.echoiot.server.service.security.exception.JwtExpiredTokenException;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TokenOutdatingTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ComponentScan({"org.echoiot.server"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DaoSqlTest
@TestPropertySource(properties = {
        "security.jwt.tokenIssuer=test.io",
        "security.jwt.tokenSigningKey=secret",
        "security.jwt.tokenExpirationTime=600",
        "security.jwt.refreshTokenExpTime=15",
        // explicitly set the wrong value to check that it is NOT used.
        "cache.specs.userSessionsInvalidation.timeToLiveInMinutes=2"
})
public class TokenOutdatingTest {
    private JwtAuthenticationProvider accessTokenAuthenticationProvider;
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    @Resource
    private TokenOutdatingService tokenOutdatingService;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Resource
    private JwtTokenFactory tokenFactory;
    private SecurityUser securityUser;

    @Before
    public void setUp() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = createMockSecurityUser(userId);

        UserService userService = mock(UserService.class);

        User user = new User();
        user.setId(userId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("email");
        when(userService.findUserById(any(), eq(userId))).thenReturn(user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        when(userService.findUserCredentialsByUserId(any(), eq(userId))).thenReturn(userCredentials);

        accessTokenAuthenticationProvider = new JwtAuthenticationProvider(tokenFactory, tokenOutdatingService);
        refreshTokenAuthenticationProvider = new RefreshTokenAuthenticationProvider(tokenFactory, userService, mock(CustomerService.class), tokenOutdatingService);
    }

    @Test
    public void testOutdateOldUserTokens() throws Exception {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        // Token outdatage time is rounded to 1 sec. Need to wait before outdating so that outdatage time is strictly after token issue time
        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));

        SECONDS.sleep(1);

        JwtToken newJwtToken = tokenFactory.createAccessJwtToken(securityUser);
        assertFalse(tokenOutdatingService.isOutdated(newJwtToken, securityUser.getId()));
    }

    @Test
    public void testAuthenticateWithOutdatedAccessToken() throws InterruptedException {
        RawAccessJwtToken accessJwtToken = getRawJwtToken(tokenFactory.createAccessJwtToken(securityUser));

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });
    }

    @Test
    public void testAuthenticateWithOutdatedRefreshToken() throws InterruptedException {
        RawAccessJwtToken refreshJwtToken = getRawJwtToken(tokenFactory.createRefreshToken(securityUser));

        assertDoesNotThrow(() -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(CredentialsExpiredException.class, () -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });
    }

    // This test takes too long to run and is basically testing the cache logic
//    @Test
//    public void testTokensOutdatageTimeRemovalFromCache() throws Exception {
//        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);
//
//        SECONDS.sleep(1);
//        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
//
//        SECONDS.sleep(1);
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30); // refreshTokenExpTime/2
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30 + 1); // refreshTokenExpTime/2 + 1
//
//        assertFalse(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//    }

    @Test
    public void testOnlyOneTokenExpired() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserSessionInvalidationEvent(securityUser.getSessionId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }

    @Test
    public void testResetAllSessions() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }


    private RawAccessJwtToken getRawJwtToken(JwtToken token) {
        return new RawAccessJwtToken(token.getToken());
    }

    private SecurityUser createMockSecurityUser(UserId userId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setEmail("email");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setAuthority(Authority.CUSTOMER_USER);
        securityUser.setId(userId);
        securityUser.setSessionId(UUID.randomUUID().toString());
        return securityUser;
    }
}
