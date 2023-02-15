package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
@ApiModel
public class EntityRelationsQuery {

    @ApiModelProperty(position = 2, value = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @ApiModelProperty(position = 1, value = "Main filters.")
    private List<RelationEntityTypeFilter> filters;

}
