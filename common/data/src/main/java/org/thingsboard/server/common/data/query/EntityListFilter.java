package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
public class EntityListFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_LIST;
    }

    private EntityType entityType;

    private List<String> entityList;

}
