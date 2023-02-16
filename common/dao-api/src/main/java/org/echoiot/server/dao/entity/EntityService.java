package org.echoiot.server.dao.entity;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.EntityCountQuery;
import org.echoiot.server.common.data.query.EntityData;
import org.echoiot.server.common.data.query.EntityDataQuery;

public interface EntityService {

    ListenableFuture<String> fetchEntityNameAsync(TenantId tenantId, EntityId entityId);

    CustomerId fetchEntityCustomerId(TenantId tenantId, EntityId entityId);

    long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query);

    PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query);

}
