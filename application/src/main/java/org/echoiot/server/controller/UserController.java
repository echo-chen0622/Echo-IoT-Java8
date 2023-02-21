package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.rule.engine.api.MailService;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.user.TbUserService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static org.echoiot.server.controller.ControllerConstants.*;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";

    @Value("${security.user_token_access_enabled}")
    @Getter
    private boolean userTokenAccessEnabled;

    @NotNull
    private final MailService mailService;
    @NotNull
    private final JwtTokenFactory tokenFactory;
    @NotNull
    private final SystemSecurityService systemSecurityService;
    @NotNull
    private final ApplicationEventPublisher eventPublisher;
    @NotNull
    private final TbUserService tbUserService;

    @NotNull
    @ApiOperation(value = "Get User (getUserById)",
            notes = "Fetch the User object based on the provided User Id. " +
                    "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User getUserById(
            @NotNull @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws EchoiotException {
        checkParameter(USER_ID, strUserId);
        try {
            @NotNull UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            if (user.getAdditionalInfo().isObject()) {
                ObjectNode additionalInfo = (ObjectNode) user.getAdditionalInfo();
                processDashboardIdFromAdditionalInfo(additionalInfo, DEFAULT_DASHBOARD);
                processDashboardIdFromAdditionalInfo(additionalInfo, HOME_DASHBOARD);
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                if (userCredentials.isEnabled() && !additionalInfo.has("userCredentialsEnabled")) {
                    additionalInfo.put("userCredentialsEnabled", true);
                }
            }
            return user;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check Token Access Enabled (isUserTokenAccessEnabled)",
            notes = "Checks that the system is configured to allow administrators to impersonate themself as other users. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to login as any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to login as any customer user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/tokenAccessEnabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    @ApiOperation(value = "Get User Token (getUserToken)",
            notes = "Returns the token of the User based on the provided User Id. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to get the token of any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to get the token of any customer user that belongs to the same tenant. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/token", method = RequestMethod.GET)
    @ResponseBody
    public JwtPair getUserToken(
            @NotNull @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws EchoiotException {
        checkParameter(USER_ID, strUserId);
        try {
            if (!userTokenAccessEnabled) {
                throw new EchoiotException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                                               EchoiotErrorCode.PERMISSION_DENIED);
            }
            @NotNull UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            User user = checkUserId(userId, Operation.READ);
            @NotNull UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            UserCredentials credentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), userId);
            @NotNull SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
            return tokenFactory.createTokenPair(securityUser);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save Or update User (saveUser)",
            notes = "Create or update the User. When creating user, platform generates User Id as " + UUID_WIKI_LINK +
                    "The newly created User Id will be present in the response. " +
                    "Specify existing User Id to update the device. " +
                    "Referencing non-existing User Id will cause 'Not Found' error." +
                    "\n\nDevice email is unique for entire platform setup." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new User entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN', 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public User saveUser(
            @NotNull @ApiParam(value = "A JSON value representing the User.", required = true)
            @RequestBody User user,
            @ApiParam(value = "Send activation email (or use activation link)", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail, HttpServletRequest request) throws EchoiotException {
        if (!Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            user.setTenantId(getCurrentUser().getTenantId());
        }
        checkEntity(user.getId(), user, PerResource.USER);
        return tbUserService.save(getTenantId(), getCurrentUser().getCustomerId(), user, sendActivationMail, request, getCurrentUser());
    }

    @ApiOperation(value = "Send or re-send the activation email",
            notes = "Force send the activation email to the user. Useful to resend the email if user has accidentally deleted it. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/sendActivationMail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @ApiParam(value = "Email of the user", required = true)
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws EchoiotException {
        try {
            User user = checkNotNull(userService.findUserByEmail(getCurrentUser().getTenantId(), email));

            accessControlService.checkPermission(getCurrentUser(), PerResource.USER, Operation.READ,
                                                 user.getId(), user);

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
            if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
                String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(activateUrl, email);
            } else {
                throw new EchoiotException("User is already activated!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the activation link (getActivationLink)",
            notes = "Get the activation link for the user. " +
                    "The base url for activation link is configurable in the general settings of system administrator. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/activationLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @NotNull @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws EchoiotException {
        checkParameter(USER_ID, strUserId);
        try {
            @NotNull UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            SecurityUser authUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), user.getId());
            if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
                String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                return activateUrl;
            } else {
                throw new EchoiotException("User is already activated!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete User (deleteUser)",
            notes = "Deletes the User, it's credentials and all the relations (from and to the User). " +
                    "Referencing non-existing User Id will cause an error. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(
            @NotNull @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws EchoiotException {
        checkParameter(USER_ID, strUserId);
        @NotNull UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.DELETE);
        if (user.getAuthority() == Authority.SYS_ADMIN && getCurrentUser().getId().equals(userId)) {
            throw new EchoiotException("Sysadmin is not allowed to delete himself", EchoiotErrorCode.PERMISSION_DENIED);
        }
        tbUserService.delete(getTenantId(), getCurrentUser().getCustomerId(), user, getCurrentUser());
    }

    @ApiOperation(value = "Get Users (getUsers)",
            notes = "Returns a page of users owned by tenant or customer. The scope depends on authority of the user that performs the request." +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUsers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            SecurityUser currentUser = getCurrentUser();
            if (Authority.TENANT_ADMIN.equals(currentUser.getAuthority())) {
                return checkNotNull(userService.findUsersByTenantId(currentUser.getTenantId(), pageLink));
            } else {
                return checkNotNull(userService.findCustomerUsers(currentUser.getTenantId(), currentUser.getCustomerId(), pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Users (getTenantAdmins)",
            notes = "Returns a page of users owned by tenant. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getTenantAdmins(
            @NotNull @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("tenantId", strTenantId);
        try {
            @NotNull TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(userService.findTenantAdmins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Users (getCustomerUsers)",
            notes = "Returns a page of users owned by customer. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getCustomerUsers(
            @NotNull @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(userService.findCustomerUsers(tenantId, customerId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Enable/Disable User credentials (setUserCredentialsEnabled)",
            notes = "Enables or Disables user credentials. Useful when you would like to block user account without deleting it. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/userCredentialsEnabled", method = RequestMethod.POST)
    @ResponseBody
    public void setUserCredentialsEnabled(
            @NotNull @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            @ApiParam(value = "Disable (\"true\") or enable (\"false\") the credentials.", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean userCredentialsEnabled) throws EchoiotException {
        checkParameter(USER_ID, strUserId);
        try {
            @NotNull UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.WRITE);
            TenantId tenantId = getCurrentUser().getTenantId();
            userService.setUserCredentialsEnabled(tenantId, userId, userCredentialsEnabled);

            if (!userCredentialsEnabled) {
                eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
