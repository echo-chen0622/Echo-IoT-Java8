package org.echoiot.server.service.entitiy.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleListener;

import java.util.List;

public interface TbEntityViewService extends ComponentLifecycleListener {

    EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws Exception;

    void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException;

    void delete(EntityView entity, User user) throws ThingsboardException;

    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    EntityView assignEntityViewToPublicCustomer(TenantId tenantId, CustomerId customerId, Customer publicCustomer,
                                                EntityViewId entityViewId, User user) throws ThingsboardException;

    EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException;

    EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView, Edge edge, User user) throws ThingsboardException;

    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);
}
