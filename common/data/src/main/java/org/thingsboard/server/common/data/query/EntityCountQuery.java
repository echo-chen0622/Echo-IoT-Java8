package org.thingsboard.server.common.data.query;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@ApiModel
@ToString
public class EntityCountQuery {

    @Getter
    private EntityFilter entityFilter;

    @Getter
    protected List<KeyFilter> keyFilters;

    public EntityCountQuery() {
    }

    public EntityCountQuery(EntityFilter entityFilter) {
        this(entityFilter, Collections.emptyList());
    }

    public EntityCountQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        this.entityFilter = entityFilter;
        this.keyFilters = keyFilters;
    }
}
