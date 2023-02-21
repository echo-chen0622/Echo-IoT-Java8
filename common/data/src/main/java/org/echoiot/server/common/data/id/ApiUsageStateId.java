package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ApiModel
public class ApiUsageStateId extends UUIDBased implements EntityId {

    @JsonCreator
    public ApiUsageStateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static ApiUsageStateId fromString(@NotNull String userId) {
        return new ApiUsageStateId(UUID.fromString(userId));
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "API_USAGE_STATE", allowableValues = "API_USAGE_STATE")
    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
