package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

@Data
public class EntityNameFilter implements EntityFilter {
    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_NAME;
    }

    private EntityType entityType;

    private String entityNameFilter;

}
