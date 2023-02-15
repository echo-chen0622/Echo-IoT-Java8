package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;
import java.util.Set;

@Data
public class RelationsQueryFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.RELATIONS_QUERY;
    }

    private EntityId rootEntity;
    private boolean isMultiRoot;
    private EntityType multiRootEntitiesType;
    private Set<String> multiRootEntityIds;
    private EntitySearchDirection direction;
    private List<RelationEntityTypeFilter> filters;
    private int maxLevel;
    private boolean fetchLastLevelOnly;

}
