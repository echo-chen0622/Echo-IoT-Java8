package org.echoiot.server.dao.entityview;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor
class EntityViewEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final EntityViewId id;
    @NotNull
    private final EntityId newEntityId;
    @NotNull
    private final EntityId oldEntityId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;

}
