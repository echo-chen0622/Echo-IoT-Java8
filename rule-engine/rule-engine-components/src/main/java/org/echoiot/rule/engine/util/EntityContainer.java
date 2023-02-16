package org.echoiot.rule.engine.util;

import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;

@Data
public class EntityContainer {

    private EntityId entityId;
    private EntityType entityType;

}
