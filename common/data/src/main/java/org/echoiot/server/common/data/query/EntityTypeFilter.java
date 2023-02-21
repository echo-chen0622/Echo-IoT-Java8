package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

@Data
public class EntityTypeFilter implements EntityFilter {
    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_TYPE;
    }

    private EntityType entityType;

}
