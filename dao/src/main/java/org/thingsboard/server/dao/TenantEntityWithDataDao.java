package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.id.TenantId;

public interface TenantEntityWithDataDao {

    Long sumDataSizeByTenantId(TenantId tenantId);
}
