package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.customer.CustomerDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.user.UserDao;
import org.echoiot.server.dao.user.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class UserDataValidator extends DataValidator<User> {

    @Resource
    private UserDao userDao;

    @Resource
    @Lazy
    private UserService userService;

    @Resource
    private CustomerDao customerDao;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Resource
    @Lazy
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull User user) {
        if (!user.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxUsers = profileConfiguration.getMaxUsers();
            validateNumberOfEntitiesPerTenant(tenantId, userDao, maxUsers, EntityType.USER);
        }
    }

    @NotNull
    @Override
    protected User validateUpdate(TenantId tenantId, @NotNull User user) {
        User old = userDao.findById(user.getTenantId(), user.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing user!");
        }
        if (!old.getTenantId().equals(user.getTenantId())) {
            throw new DataValidationException("Can't update user tenant id!");
        }
        if (!old.getAuthority().equals(user.getAuthority())) {
            throw new DataValidationException("Can't update user authority!");
        }
        if (!old.getCustomerId().equals(user.getCustomerId())) {
            throw new DataValidationException("Can't update user customer id!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId requestTenantId, @NotNull User user) {
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new DataValidationException("User email should be specified!");
        }

        validateEmail(user.getEmail());

        Authority authority = user.getAuthority();
        if (authority == null) {
            throw new DataValidationException("User authority isn't defined!");
        }
        TenantId tenantId = user.getTenantId();
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
            user.setTenantId(tenantId);
        }
        CustomerId customerId = user.getCustomerId();
        if (customerId == null) {
            customerId = new CustomerId(ModelConstants.NULL_UUID);
            user.setCustomerId(customerId);
        }

        switch (authority) {
            case SYS_ADMIN:
                if (!tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || !customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("System administrator can't be assigned neither to tenant nor to customer!");
                }
                break;
            case TENANT_ADMIN:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator should be assigned to tenant!");
                } else if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator can't be assigned to customer!");
                }
                break;
            case CUSTOMER_USER:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Customer user should be assigned to customer!");
                }
                break;
            default:
                break;
        }

        User existentUserWithEmail = userService.findUserByEmail(tenantId, user.getEmail());
        if (existentUserWithEmail != null && !isSameData(existentUserWithEmail, user)) {
            throw new DataValidationException("User with email '" + user.getEmail() + "' "
                    + " already present in database!");
        }
        if (!tenantId.getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(user.getTenantId())) {
                throw new DataValidationException("User is referencing to non-existent tenant!");
            }
        }
        if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, user.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("User is referencing to non-existent customer!");
            } else if (!customer.getTenantId().getId().equals(tenantId.getId())) {
                throw new DataValidationException("User can't be assigned to customer from different tenant!");
            }
        }
    }
}
