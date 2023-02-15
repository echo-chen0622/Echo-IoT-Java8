package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class EntityViewTypeFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_VIEW_TYPE;
    }

    private String entityViewType;

    private String entityViewNameFilter;

}
