package org.thingsboard.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

public final class WidgetTypeId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public WidgetTypeId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "WIDGET_TYPE", allowableValues = "WIDGET_TYPE")
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }
}
