package org.echoiot.server.service.apiusage;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;

public class CustomerApiUsageState extends BaseApiUsageState {
    public CustomerApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }
}
