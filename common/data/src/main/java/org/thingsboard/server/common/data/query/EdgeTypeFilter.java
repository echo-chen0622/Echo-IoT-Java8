package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class EdgeTypeFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.EDGE_TYPE;
    }

    private String edgeType;

    private String edgeNameFilter;

}
