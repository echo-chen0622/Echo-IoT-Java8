package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class EdgeTypeFilter implements EntityFilter {

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.EDGE_TYPE;
    }

    private String edgeType;

    private String edgeNameFilter;

}
