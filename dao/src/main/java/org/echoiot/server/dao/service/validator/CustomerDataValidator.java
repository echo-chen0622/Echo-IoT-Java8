package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.customer.CustomerDao;
import org.echoiot.server.dao.customer.CustomerServiceImpl;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerDataValidator extends DataValidator<Customer> {

    @Resource
    private CustomerDao customerDao;

    @Resource
    private TenantService tenantService;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull Customer customer) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxCustomers = profileConfiguration.getMaxCustomers();

        validateNumberOfEntitiesPerTenant(tenantId, customerDao, maxCustomers, EntityType.CUSTOMER);
        customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                c -> {
                    throw new DataValidationException("Customer with such title already exists!");
                }
        );
    }

    @Nullable
    @Override
    protected Customer validateUpdate(TenantId tenantId, @NotNull Customer customer) {
        Optional<Customer> customerOpt = customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle());
        customerOpt.ifPresent(
                c -> {
                    if (!c.getId().equals(customer.getId())) {
                        throw new DataValidationException("Customer with such title already exists!");
                    }
                }
        );
        return customerOpt.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull Customer customer) {
        if (StringUtils.isEmpty(customer.getTitle())) {
            throw new DataValidationException("Customer title should be specified!");
        }
        if (customer.getTitle().equals(CustomerServiceImpl.PUBLIC_CUSTOMER_TITLE)) {
            throw new DataValidationException("'Public' title for customer is system reserved!");
        }
        if (!StringUtils.isEmpty(customer.getEmail())) {
            validateEmail(customer.getEmail());
        }
        if (customer.getTenantId() == null) {
            throw new DataValidationException("Customer should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(customer.getTenantId())) {
                throw new DataValidationException("Customer is referencing to non-existent tenant!");
            }
        }
    }
}
