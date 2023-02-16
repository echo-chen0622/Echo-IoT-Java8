package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.id.EntityId;

@Data
public abstract class EntitySearchQueryFilter implements EntityFilter {

    private EntityId rootEntity;
    private String relationType;
    private EntitySearchDirection direction;
    private int maxLevel;
    private boolean fetchLastLevelOnly;

}
