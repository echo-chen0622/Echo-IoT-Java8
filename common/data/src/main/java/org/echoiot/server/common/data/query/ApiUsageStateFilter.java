package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.id.CustomerId;
import org.jetbrains.annotations.NotNull;

@Data
public class ApiUsageStateFilter implements EntityFilter {

    private CustomerId customerId;

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.API_USAGE_STATE;
    }

}
