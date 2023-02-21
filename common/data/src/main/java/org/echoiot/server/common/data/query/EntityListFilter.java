package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class EntityListFilter implements EntityFilter {
    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_LIST;
    }

    private EntityType entityType;

    private List<String> entityList;

}
