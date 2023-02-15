package org.thingsboard.server.common.data.asset;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashvayka on 03.05.17.
 */
@Data
public class AssetSearchQuery {

    @ApiModelProperty(position = 3, value = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @ApiModelProperty(position = 1, value = "Type of the relation between root entity and asset (e.g. 'Contains' or 'Manages').")
    private String relationType;
    @ApiModelProperty(position = 2, value = "Array of asset types to filter the related entities (e.g. 'Building', 'Vehicle').")
    private List<String> assetTypes;

    public EntityRelationsQuery toEntitySearchQuery() {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(parameters);
        query.setFilters(
                Collections.singletonList(new RelationEntityTypeFilter(relationType == null ? EntityRelation.CONTAINS_TYPE : relationType,
                        Collections.singletonList(EntityType.ASSET))));
        return query;
    }
}
