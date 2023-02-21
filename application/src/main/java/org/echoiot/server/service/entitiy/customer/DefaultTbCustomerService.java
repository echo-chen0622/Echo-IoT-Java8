package org.echoiot.server.service.entitiy.customer;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbCustomerService extends AbstractTbEntityService implements TbCustomerService {

    @NotNull
    @Override
    public Customer save(@NotNull Customer customer, User user) throws Exception {
        @NotNull ActionType actionType = customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = customer.getTenantId();
        try {
            Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));
            autoCommit(user, savedCustomer.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedCustomer.getId(), savedCustomer, null, actionType, user);
            return savedCustomer;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), customer, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(@NotNull Customer customer, User user) {
        TenantId tenantId = customer.getTenantId();
        CustomerId customerId = customer.getId();
        try {
            @Nullable List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, customer.getId());
            customerService.deleteCustomer(tenantId, customerId);
            notificationEntityService.notifyDeleteEntity(tenantId, customer.getId(), customer, customerId,
                    ActionType.DELETED, relatedEdgeIds, user, customerId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, customer.getId(), ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), ActionType.DELETED,
                    user, e, customerId.toString());
            throw e;
        }
    }
}
