package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class EntityViewTypeFilter implements EntityFilter {

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_VIEW_TYPE;
    }

    private String entityViewType;

    private String entityViewNameFilter;

}
