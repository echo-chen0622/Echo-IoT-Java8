package org.echoiot.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

public class DeviceProfileId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public DeviceProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static DeviceProfileId fromString(@NotNull String deviceProfileId) {
        return new DeviceProfileId(UUID.fromString(deviceProfileId));
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "DEVICE_PROFILE", allowableValues = "DEVICE_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }
}
