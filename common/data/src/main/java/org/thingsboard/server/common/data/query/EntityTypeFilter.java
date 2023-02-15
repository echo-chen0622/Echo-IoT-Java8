package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Data
public class EntityTypeFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_TYPE;
    }

    private EntityType entityType;

}
