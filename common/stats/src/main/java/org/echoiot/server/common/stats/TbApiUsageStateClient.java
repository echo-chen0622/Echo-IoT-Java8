package org.echoiot.server.common.stats;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.TenantId;

public interface TbApiUsageStateClient {

    ApiUsageState getApiUsageState(TenantId tenantId);

}
