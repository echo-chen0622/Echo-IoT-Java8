package org.echoiot.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by Echo on 03.05.17.
 */
@ApiModel
@Data
@AllArgsConstructor
public class RelationsSearchParameters {

    @ApiModelProperty(position = 1, value = "Root entity id to start search from.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private UUID rootId;
    @ApiModelProperty(position = 2, value = "Type of the root entity.")
    private EntityType rootType;
    @ApiModelProperty(position = 3, value = "Type of the root entity.")
    private EntitySearchDirection direction;
    @ApiModelProperty(position = 4, value = "Type of the relation.")
    private RelationTypeGroup relationTypeGroup;
    @ApiModelProperty(position = 5, value = "Maximum level of the search depth.")
    private int maxLevel = 1;
    @ApiModelProperty(position = 6, value = "Fetch entities that match the last level of search. Useful to find Devices that are strictly 'maxLevel' relations away from the root entity.")
    private boolean fetchLastLevelOnly;

    public RelationsSearchParameters(@NotNull EntityId entityId, EntitySearchDirection direction, int maxLevel, boolean fetchLastLevelOnly) {
        this(entityId, direction, maxLevel, RelationTypeGroup.COMMON, fetchLastLevelOnly);
    }

    public RelationsSearchParameters(@NotNull EntityId entityId, EntitySearchDirection direction, int maxLevel, RelationTypeGroup relationTypeGroup, boolean fetchLastLevelOnly) {
        this.rootId = entityId.getId();
        this.rootType = entityId.getEntityType();
        this.direction = direction;
        this.maxLevel = maxLevel;
        this.relationTypeGroup = relationTypeGroup;
        this.fetchLastLevelOnly = fetchLastLevelOnly;
    }

    @JsonIgnore
    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(rootType, rootId);
    }
}
