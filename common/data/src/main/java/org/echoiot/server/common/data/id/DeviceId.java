package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ApiModel
public class DeviceId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public DeviceId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static DeviceId fromString(@NotNull String deviceId) {
        return new DeviceId(UUID.fromString(deviceId));
    }

    @NotNull
    @Override
    @ApiModelProperty(position = 2, required = true, value = "string", example = "DEVICE", allowableValues = "DEVICE")
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }
}
