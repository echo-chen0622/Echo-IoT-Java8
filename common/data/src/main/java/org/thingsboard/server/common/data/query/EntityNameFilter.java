package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Data
public class EntityNameFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_NAME;
    }

    private EntityType entityType;

    private String entityNameFilter;

}
