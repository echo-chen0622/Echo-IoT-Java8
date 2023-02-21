package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public class EntityViewId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EntityViewId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static EntityViewId fromString(@NotNull String entityViewID) {
        return new EntityViewId(UUID.fromString(entityViewID));
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "ENTITY_VIEW", allowableValues = "ENTITY_VIEW")
    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }
}
