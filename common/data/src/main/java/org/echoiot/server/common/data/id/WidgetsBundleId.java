package org.echoiot.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;

public final class WidgetsBundleId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public WidgetsBundleId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "WIDGETS_BUNDLE", allowableValues = "WIDGETS_BUNDLE")
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }
}
