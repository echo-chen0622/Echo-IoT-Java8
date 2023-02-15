package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class OtaPackageId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public OtaPackageId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OtaPackageId fromString(String firmwareId) {
        return new OtaPackageId(UUID.fromString(firmwareId));
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "OTA_PACKAGE", allowableValues = "OTA_PACKAGE")
    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
