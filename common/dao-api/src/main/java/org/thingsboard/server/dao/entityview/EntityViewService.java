package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public interface EntityViewService {

    EntityView saveEntityView(EntityView entityView);

    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId);

    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId);

    void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId);

    EntityViewInfo findEntityViewInfoById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    List<EntityView> findEntityViewsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId);

    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    void deleteEntityViewsByTenantId(TenantId tenantId);

    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);

    EntityView assignEntityViewToEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId);

    EntityView unassignEntityViewFromEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId);

    PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);
}
