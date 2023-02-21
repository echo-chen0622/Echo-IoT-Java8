package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TbResourceId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public TbResourceId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "TB_RESOURCE", allowableValues = "TB_RESOURCE")
    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }
}
