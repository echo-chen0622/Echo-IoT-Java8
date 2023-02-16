package org.echoiot.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

import java.util.Optional;

public interface CustomerService {

    Customer findCustomerById(TenantId tenantId, CustomerId customerId);

    Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title);

    ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId);

    Customer saveCustomer(Customer customer);

    void deleteCustomer(TenantId tenantId, CustomerId customerId);

    Customer findOrCreatePublicCustomer(TenantId tenantId);

    PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteCustomersByTenantId(TenantId tenantId);

}
