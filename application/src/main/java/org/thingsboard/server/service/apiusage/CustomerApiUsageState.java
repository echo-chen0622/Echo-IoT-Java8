package org.thingsboard.server.service.apiusage;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;

public class CustomerApiUsageState extends BaseApiUsageState {
    public CustomerApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }
}
