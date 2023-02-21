package org.echoiot.server.service.security.model.token;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.common.data.security.model.JwtToken;
import org.echoiot.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.echoiot.server.service.security.exception.JwtExpiredTokenException;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFactory {

    private static final String SCOPES = "scopes";
    private static final String USER_ID = "userId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ENABLED = "enabled";
    private static final String IS_PUBLIC = "isPublic";
    private static final String TENANT_ID = "tenantId";
    private static final String CUSTOMER_ID = "customerId";
    private static final String SESSION_ID = "sessionId";

    @NotNull
    private final JwtSettingsService jwtSettingsService;

    /**
     * Factory method for issuing new JWT Tokens.
     */
    @NotNull
    public AccessJwtToken createAccessJwtToken(@NotNull SecurityUser securityUser) {
        if (securityUser.getAuthority() == null) {
            throw new IllegalArgumentException("User doesn't have any privileges");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        JwtBuilder jwtBuilder = setUpToken(securityUser, securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()), jwtSettingsService.getJwtSettings().getTokenExpirationTime());
        jwtBuilder.claim(FIRST_NAME, securityUser.getFirstName())
                .claim(LAST_NAME, securityUser.getLastName())
                .claim(ENABLED, securityUser.isEnabled())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID);
        if (securityUser.getTenantId() != null) {
            jwtBuilder.claim(TENANT_ID, securityUser.getTenantId().getId().toString());
        }
        if (securityUser.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, securityUser.getCustomerId().getId().toString());
        }

        String token = jwtBuilder.compact();

        return new AccessJwtToken(token);
    }

    @NotNull
    public SecurityUser parseAccessJwtToken(@NotNull RawAccessJwtToken rawAccessToken) {
        Jws<Claims> jwsClaims = parseTokenClaims(rawAccessToken);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT Token doesn't have any scopes");
        }

        @NotNull SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setEmail(subject);
        securityUser.setAuthority(Authority.parse(scopes.get(0)));
        String tenantId = claims.get(TENANT_ID, String.class);
        if (tenantId != null) {
            securityUser.setTenantId(TenantId.fromUUID(UUID.fromString(tenantId)));
        } else if (securityUser.getAuthority() == Authority.SYS_ADMIN) {
            securityUser.setTenantId(TenantId.SYS_TENANT_ID);
        }
        String customerId = claims.get(CUSTOMER_ID, String.class);
        if (customerId != null) {
            securityUser.setCustomerId(new CustomerId(UUID.fromString(customerId)));
        }
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }

        UserPrincipal principal;
        if (securityUser.getAuthority() != Authority.PRE_VERIFICATION_TOKEN) {
            securityUser.setFirstName(claims.get(FIRST_NAME, String.class));
            securityUser.setLastName(claims.get(LAST_NAME, String.class));
            securityUser.setEnabled(claims.get(ENABLED, Boolean.class));
            boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
            principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        } else {
            principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, subject);
        }
        securityUser.setUserPrincipal(principal);

        return securityUser;
    }

    @NotNull
    public JwtToken createRefreshToken(@NotNull SecurityUser securityUser) {
        UserPrincipal principal = securityUser.getUserPrincipal();

        String token = setUpToken(securityUser, Collections.singletonList(Authority.REFRESH_TOKEN.name()), jwtSettingsService.getJwtSettings().getRefreshTokenExpTime())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID)
                .setId(UUID.randomUUID().toString()).compact();

        return new AccessJwtToken(token);
    }

    @NotNull
    public SecurityUser parseRefreshToken(@NotNull RawAccessJwtToken rawAccessToken) {
        Jws<Claims> jwsClaims = parseTokenClaims(rawAccessToken);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Refresh Token doesn't have any scopes");
        }
        if (!scopes.get(0).equals(Authority.REFRESH_TOKEN.name())) {
            throw new IllegalArgumentException("Invalid Refresh Token scope");
        }
        boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
        @NotNull UserPrincipal principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        @NotNull SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setUserPrincipal(principal);
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }
        return securityUser;
    }

    @NotNull
    public JwtToken createPreVerificationToken(@NotNull SecurityUser user, Integer expirationTime) {
        JwtBuilder jwtBuilder = setUpToken(user, Collections.singletonList(Authority.PRE_VERIFICATION_TOKEN.name()), expirationTime)
                .claim(TENANT_ID, user.getTenantId().toString());
        if (user.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, user.getCustomerId().toString());
        }
        return new AccessJwtToken(jwtBuilder.compact());
    }

    private JwtBuilder setUpToken(@NotNull SecurityUser securityUser, List<String> scopes, long expirationTime) {
        if (StringUtils.isBlank(securityUser.getEmail())) {
            throw new IllegalArgumentException("Cannot create JWT Token without username/email");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        Claims claims = Jwts.claims().setSubject(principal.getValue());
        claims.put(USER_ID, securityUser.getId().getId().toString());
        claims.put(SCOPES, scopes);
        if (securityUser.getSessionId() != null) {
            claims.put(SESSION_ID, securityUser.getSessionId());
        }

        @NotNull ZonedDateTime currentTime = ZonedDateTime.now();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtSettingsService.getJwtSettings().getTokenIssuer())
                .setIssuedAt(Date.from(currentTime.toInstant()))
                .setExpiration(Date.from(currentTime.plusSeconds(expirationTime).toInstant()))
                .signWith(SignatureAlgorithm.HS512, jwtSettingsService.getJwtSettings().getTokenSigningKey());
    }

    public Jws<Claims> parseTokenClaims(@NotNull JwtToken token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSettingsService.getJwtSettings().getTokenSigningKey())
                    .parseClaimsJws(token.getToken());
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (SignatureException | ExpiredJwtException expiredEx) {
            log.debug("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(token, "JWT Token expired", expiredEx);
        }
    }

    @NotNull
    public JwtPair createTokenPair(@NotNull SecurityUser securityUser) {
        @NotNull JwtToken accessToken = createAccessJwtToken(securityUser);
        @NotNull JwtToken refreshToken = createRefreshToken(securityUser);
        return new JwtPair(accessToken.getToken(), refreshToken.getToken());
    }

}
