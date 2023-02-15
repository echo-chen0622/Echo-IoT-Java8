package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
@AllArgsConstructor
@ApiModel
public class RelationEntityTypeFilter {

    @ApiModelProperty(position = 1, value = "Type of the relation between root entity and other entity (e.g. 'Contains' or 'Manages').", example = "Contains")
    private String relationType;

    @ApiModelProperty(position = 2, value = "Array of entity types to filter the related entities (e.g. 'DEVICE', 'ASSET').")
    private List<EntityType> entityTypes;
}
