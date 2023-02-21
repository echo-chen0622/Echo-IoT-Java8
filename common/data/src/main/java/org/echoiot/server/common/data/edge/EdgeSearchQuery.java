package org.echoiot.server.common.data.edge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntityRelationsQuery;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;
import org.echoiot.server.common.data.relation.RelationsSearchParameters;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Data
public class EdgeSearchQuery {

    @ApiModelProperty(position = 3, value = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @ApiModelProperty(position = 1, value = "Type of the relation between root entity and edge (e.g. 'Contains' or 'Manages').")
    private String relationType;
    @ApiModelProperty(position = 2, value = "Array of edge types to filter the related entities (e.g. 'Silos', 'Stores').")
    private List<String> edgeTypes;

    @NotNull
    public EntityRelationsQuery toEntitySearchQuery() {
        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(parameters);
        query.setFilters(
                Collections.singletonList(new RelationEntityTypeFilter(relationType == null ? EntityRelation.CONTAINS_TYPE : relationType,
                                                                       Collections.singletonList(EntityType.EDGE))));
        return query;
    }
}
