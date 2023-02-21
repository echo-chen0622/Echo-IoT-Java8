package org.echoiot.server.common.data.query;

import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@Data
public class RelationsQueryFilter implements EntityFilter {

    @NotNull
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
