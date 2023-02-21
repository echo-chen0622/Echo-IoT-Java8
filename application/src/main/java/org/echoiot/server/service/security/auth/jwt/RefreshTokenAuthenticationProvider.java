package org.echoiot.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.echoiot.server.service.security.auth.RefreshAuthenticationToken;
import org.echoiot.server.service.security.auth.TokenOutdatingService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {
    @NotNull
    private final JwtTokenFactory tokenFactory;
    @NotNull
    private final UserService userService;
    @NotNull
    private final CustomerService customerService;
    @NotNull
    private final TokenOutdatingService tokenOutdatingService;

    @NotNull
    @Override
    public Authentication authenticate(@NotNull Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken);
        UserPrincipal principal = unsafeUser.getUserPrincipal();

        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(unsafeUser.getId());
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        securityUser.setSessionId(unsafeUser.getSessionId());
        if (tokenOutdatingService.isOutdated(rawAccessToken, securityUser.getId())) {
            throw new CredentialsExpiredException("Token is outdated");
        }

        return new RefreshAuthenticationToken(securityUser);
    }

    @NotNull
    private SecurityUser authenticateByUserId(UserId userId) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        User user = userService.findUserById(systemId, userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found by refresh token");
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(systemId, user.getId());
        if (userCredentials == null) {
            throw new UsernameNotFoundException("User credentials not found");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        if (user.getAuthority() == null) throw new InsufficientAuthenticationException("User has no authority assigned");

        @NotNull UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        @NotNull SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);

        return securityUser;
    }

    @NotNull
    private SecurityUser authenticateByPublicId(@NotNull String publicId) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found by refresh token");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Refresh token is not valid");
        }

        @NotNull User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        @NotNull UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId);

        @NotNull SecurityUser securityUser = new SecurityUser(user, true, userPrincipal);

        return securityUser;
    }

    @Override
    public boolean supports(@NotNull Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
