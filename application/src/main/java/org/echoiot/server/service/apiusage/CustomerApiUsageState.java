package org.echoiot.server.service.apiusage;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

public class CustomerApiUsageState extends BaseApiUsageState {
    public CustomerApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }
}
