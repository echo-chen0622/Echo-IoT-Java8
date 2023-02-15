package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.TenantId;

public interface RuleEngineApiUsageStateService {

    ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id);

}
