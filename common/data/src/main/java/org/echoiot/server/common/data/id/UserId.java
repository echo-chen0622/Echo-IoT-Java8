package org.echoiot.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

public class UserId extends UUIDBased implements EntityId {

    @JsonCreator
    public UserId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static UserId fromString(@NotNull String userId) {
        return new UserId(UUID.fromString(userId));
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "USER", allowableValues = "USER")
    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
