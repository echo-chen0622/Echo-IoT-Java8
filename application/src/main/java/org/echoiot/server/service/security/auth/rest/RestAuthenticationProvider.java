package org.echoiot.server.service.security.auth.rest;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.MfaAuthenticationToken;
import org.echoiot.server.service.security.auth.mfa.TwoFactorAuthService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.UUID;


@Component
@Slf4j
@TbCoreComponent
public class RestAuthenticationProvider implements AuthenticationProvider {

    private final SystemSecurityService systemSecurityService;
    private final UserService userService;
    private final CustomerService customerService;
    private final TwoFactorAuthService twoFactorAuthService;

    @Autowired
    public RestAuthenticationProvider(final UserService userService,
                                      final CustomerService customerService,
                                      final SystemSecurityService systemSecurityService,
                                      TwoFactorAuthService twoFactorAuthService) {
        this.userService = userService;
        this.customerService = customerService;
        this.systemSecurityService = systemSecurityService;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @NotNull
    @Override
    public Authentication authenticate(@NotNull Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new BadCredentialsException("Authentication Failed. Bad user principal.");
        }

        @NotNull UserPrincipal userPrincipal =  (UserPrincipal) principal;
        SecurityUser securityUser;
        if (userPrincipal.getType() == UserPrincipal.Type.USER_NAME) {
            String username = userPrincipal.getValue();
            String password = (String) authentication.getCredentials();
            securityUser = authenticateByUsernameAndPassword(authentication, userPrincipal, username, password);
            if (twoFactorAuthService.isTwoFaEnabled(securityUser.getTenantId(), securityUser.getId())) {
                return new MfaAuthenticationToken(securityUser);
            } else {
                systemSecurityService.logLoginAction(securityUser, authentication.getDetails(), ActionType.LOGIN, null);
            }
        } else {
            String publicId = userPrincipal.getValue();
            securityUser = authenticateByPublicId(userPrincipal, publicId);
        }

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    @NotNull
    private SecurityUser authenticateByUsernameAndPassword(@NotNull Authentication authentication, UserPrincipal userPrincipal, String username, String password) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        try {

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
            if (userCredentials == null) {
                throw new UsernameNotFoundException("User credentials not found");
            }

            try {
                systemSecurityService.validateUserCredentials(user.getTenantId(), userCredentials, username, password);
            } catch (LockedException e) {
                systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOCKOUT, null);
                throw e;
            }

            if (user.getAuthority() == null)
                throw new InsufficientAuthenticationException("User has no authority assigned");

            return new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);
        } catch (Exception e) {
            systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOGIN, e);
            throw e;
        }
    }

    @NotNull
    private SecurityUser authenticateByPublicId(UserPrincipal userPrincipal, @NotNull String publicId) {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        Customer publicCustomer = customerService.findCustomerById(TenantId.SYS_TENANT_ID, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found: " + publicId);
        }
        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        @NotNull User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        return new SecurityUser(user, true, userPrincipal);
    }

    @Override
    public boolean supports(@NotNull Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
