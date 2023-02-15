package org.thingsboard.server.common.stats;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbApiUsageStateClient {

    ApiUsageState getApiUsageState(TenantId tenantId);

}
