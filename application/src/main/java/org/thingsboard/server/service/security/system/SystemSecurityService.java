package org.thingsboard.server.service.security.system;

import org.springframework.security.core.AuthenticationException;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;

public interface SystemSecurityService {

    SecuritySettings getSecuritySettings(TenantId tenantId);

    SecuritySettings saveSecuritySettings(TenantId tenantId, SecuritySettings securitySettings);

    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;

    void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings);

    void validatePassword(TenantId tenantId, String password, UserCredentials userCredentials) throws DataValidationException;

    String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest);

    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e);

    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e);
}
