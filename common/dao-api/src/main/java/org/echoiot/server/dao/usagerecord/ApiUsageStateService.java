package org.echoiot.server.dao.usagerecord;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.ApiUsageStateId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;

public interface ApiUsageStateService {

    ApiUsageState createDefaultApiUsageState(TenantId id, EntityId entityId);

    ApiUsageState update(ApiUsageState apiUsageState);

    ApiUsageState findTenantApiUsageState(TenantId tenantId);

    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    void deleteApiUsageStateByTenantId(TenantId tenantId);

    void deleteApiUsageStateByEntityId(EntityId entityId);

    ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id);
}
