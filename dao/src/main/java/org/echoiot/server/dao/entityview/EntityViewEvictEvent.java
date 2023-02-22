package org.echoiot.server.dao.entityview;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;

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
