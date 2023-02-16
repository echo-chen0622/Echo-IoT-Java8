package org.echoiot.server.dao.usagerecord;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.Dao;

import java.util.UUID;

public interface ApiUsageStateDao extends Dao<ApiUsageState> {

    /**
     * Save or update usage record object
     *
     * @param apiUsageState the usage record
     * @return saved usage record entity
     */
    ApiUsageState save(TenantId tenantId, ApiUsageState apiUsageState);

    /**
     * Find usage record by tenantId.
     *
     * @param tenantId the tenantId
     * @return the corresponding usage record
     */
    ApiUsageState findTenantApiUsageState(UUID tenantId);

    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    /**
     * Delete usage record by tenantId.
     *
     * @param tenantId the tenantId
     */
    void deleteApiUsageStateByTenantId(TenantId tenantId);

    void deleteApiUsageStateByEntityId(EntityId entityId);
}
