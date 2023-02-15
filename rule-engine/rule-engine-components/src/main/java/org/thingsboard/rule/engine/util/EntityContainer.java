package org.thingsboard.rule.engine.util;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

@Data
public class EntityContainer {

    private EntityId entityId;
    private EntityType entityType;

}
