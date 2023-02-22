package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;

import java.util.UUID;

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
