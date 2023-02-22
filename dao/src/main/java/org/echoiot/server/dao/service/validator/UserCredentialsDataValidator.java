package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.user.UserCredentialsDao;
import org.echoiot.server.dao.user.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class UserCredentialsDataValidator extends DataValidator<UserCredentials> {

    @Resource
    private UserCredentialsDao userCredentialsDao;

    @Resource
    @Lazy
    private UserService userService;

    @Override
    protected void validateCreate(TenantId tenantId, UserCredentials userCredentials) {
        throw new IncorrectParameterException("Creation of new user credentials is prohibited.");
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, UserCredentials userCredentials) {
        if (userCredentials.getUserId() == null) {
            throw new DataValidationException("User credentials should be assigned to user!");
        }
        if (userCredentials.isEnabled()) {
            if (StringUtils.isEmpty(userCredentials.getPassword())) {
                throw new DataValidationException("Enabled user credentials should have password!");
            }
            if (StringUtils.isNotEmpty(userCredentials.getActivateToken())) {
                throw new DataValidationException("Enabled user credentials can't have activate token!");
            }
        }
        UserCredentials existingUserCredentialsEntity = userCredentialsDao.findById(tenantId, userCredentials.getId().getId());
        if (existingUserCredentialsEntity == null) {
            throw new DataValidationException("Unable to update non-existent user credentials!");
        }
        User user = userService.findUserById(tenantId, userCredentials.getUserId());
        if (user == null) {
            throw new DataValidationException("Can't assign user credentials to non-existent user!");
        }
    }
}
