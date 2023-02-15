package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;

@Data
public class ApiUsageStateFilter implements EntityFilter {

    private CustomerId customerId;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.API_USAGE_STATE;
    }

}
