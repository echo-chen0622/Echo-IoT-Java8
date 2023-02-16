package org.echoiot.server.dao;

import org.echoiot.server.common.data.id.TenantId;

public interface TenantEntityWithDataDao {

    Long sumDataSizeByTenantId(TenantId tenantId);
}
