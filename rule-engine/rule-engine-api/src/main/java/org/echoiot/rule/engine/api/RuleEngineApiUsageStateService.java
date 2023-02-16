package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.ApiUsageStateId;
import org.echoiot.server.common.data.id.TenantId;

public interface RuleEngineApiUsageStateService {

    ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id);

}
