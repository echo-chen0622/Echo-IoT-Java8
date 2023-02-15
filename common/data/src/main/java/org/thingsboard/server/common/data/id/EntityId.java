package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */

@JsonDeserialize(using = EntityIdDeserializer.class)
@JsonSerialize(using = EntityIdSerializer.class)
@ApiModel
public interface EntityId extends HasUUID, Serializable { //NOSONAR, the constant is closely related to EntityId

    UUID NULL_UUID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    @ApiModelProperty(position = 1, required = true, value = "ID of the entity, time-based UUID v1", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    UUID getId();

    @ApiModelProperty(position = 2, required = true, example = "DEVICE")
    EntityType getEntityType();

    @JsonIgnore
    default boolean isNullUid() {
        return NULL_UUID.equals(getId());
    }

}
