package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.customer.CustomerDao;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class CustomerImportService extends BaseEntityImportService<CustomerId, Customer, EntityExportData<Customer>> {

    @NotNull
    private final CustomerService customerService;
    @NotNull
    private final CustomerDao customerDao;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull Customer customer, IdProvider idProvider) {
        customer.setTenantId(tenantId);
    }

    @NotNull
    @Override
    protected Customer prepare(@NotNull EntitiesImportCtx ctx, @NotNull Customer customer, Customer old, EntityExportData<Customer> exportData, IdProvider idProvider) {
        if (customer.isPublic()) {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(ctx.getTenantId());
            publicCustomer.setExternalId(customer.getExternalId());
            return publicCustomer;
        } else {
            return customer;
        }
    }

    @Override
    protected Customer saveOrUpdate(@NotNull EntitiesImportCtx ctx, @NotNull Customer customer, EntityExportData<Customer> exportData, IdProvider idProvider) {
        if (!customer.isPublic()) {
            return customerService.saveCustomer(customer);
        } else {
            return customerDao.save(ctx.getTenantId(), customer);
        }
    }

    @NotNull
    @Override
    protected Customer deepCopy(@NotNull Customer customer) {
        return new Customer(customer);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
