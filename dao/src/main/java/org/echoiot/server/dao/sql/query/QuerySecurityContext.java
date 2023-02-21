package org.echoiot.server.dao.sql.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class QuerySecurityContext {

    @NotNull
    @Getter
    private final TenantId tenantId;
    @NotNull
    @Getter
    private final CustomerId customerId;
    @NotNull
    @Getter
    private final EntityType entityType;
    @Getter
    private final boolean ignorePermissionCheck;

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        this(tenantId, customerId, entityType, false);
    }
}
