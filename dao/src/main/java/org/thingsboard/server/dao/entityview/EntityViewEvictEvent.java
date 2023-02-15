package org.thingsboard.server.dao.entityview;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@RequiredArgsConstructor
class EntityViewEvictEvent {

    private final TenantId tenantId;
    private final EntityViewId id;
    private final EntityId newEntityId;
    private final EntityId oldEntityId;
    private final String newName;
    private final String oldName;

}
