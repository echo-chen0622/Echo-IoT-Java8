package org.echoiot.server.dao.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.dao.user.UserService;

import java.io.IOException;
import java.util.Optional;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class CustomerServiceImpl extends AbstractEntityService implements CustomerService {

    public static final String PUBLIC_CUSTOMER_TITLE = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Resource
    private CustomerDao customerDao;

    @Resource
    private UserService userService;

    @Resource
    private AssetService assetService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DashboardService dashboardService;

    @Lazy
    @Resource
    private ApiUsageStateService apiUsageStateService;

    @Resource
    private DataValidator<Customer> customerValidator;

    @Override
    public Customer findCustomerById(TenantId tenantId, @NotNull CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findById(tenantId, customerId.getId());
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(@NotNull TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitle [{}] [{}]", tenantId, title);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), title);
    }

    @Override
    public ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, @NotNull CustomerId customerId) {
        log.trace("Executing findCustomerByIdAsync [{}]", customerId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findByIdAsync(tenantId, customerId.getId());
    }

    @NotNull
    @Override
    public Customer saveCustomer(@NotNull Customer customer) {
        log.trace("Executing saveCustomer [{}]", customer);
        customerValidator.validate(customer, Customer::getTenantId);
        try {
            Customer savedCustomer = customerDao.save(customer.getTenantId(), customer);
            dashboardService.updateCustomerDashboards(savedCustomer.getTenantId(), savedCustomer.getId());
            return savedCustomer;
        } catch (Exception e) {
            checkConstraintViolation(e, "customer_external_id_unq_key", "Customer with such external id already exists!");
            throw e;
        }

    }

    @Override
    @Transactional
    public void deleteCustomer(TenantId tenantId, @NotNull CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        dashboardService.unassignCustomerDashboards(tenantId, customerId);
        entityViewService.unassignCustomerEntityViews(customer.getTenantId(), customerId);
        assetService.unassignCustomerAssets(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        edgeService.unassignCustomerEdges(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        deleteEntityRelations(tenantId, customerId);
        apiUsageStateService.deleteApiUsageStateByEntityId(customerId);
        customerDao.removeById(tenantId, customerId.getId());
    }

    @Override
    public Customer findOrCreatePublicCustomer(@NotNull TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_CUSTOMER_ID + tenantId);
        Optional<Customer> publicCustomerOpt = customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), PUBLIC_CUSTOMER_TITLE);
        if (publicCustomerOpt.isPresent()) {
            return publicCustomerOpt.get();
        } else {
            @NotNull Customer publicCustomer = new Customer();
            publicCustomer.setTenantId(tenantId);
            publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
            try {
                publicCustomer.setAdditionalInfo(new ObjectMapper().readValue("{ \"isPublic\": true }", JsonNode.class));
            } catch (IOException e) {
                throw new IncorrectParameterException("Unable to create public customer.", e);
            }
            return customerDao.save(tenantId, publicCustomer);
        }
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(@NotNull TenantId tenantId, @NotNull PageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink);
        return customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<TenantId, Customer>() {

                @Override
                protected PageData<Customer> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()));
                }
            };
}
