package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;

import java.util.UUID;

public class AssetProfileId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AssetProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AssetProfileId fromString(String assetProfileId) {
        return new AssetProfileId(UUID.fromString(assetProfileId));
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "ASSET_PROFILE", allowableValues = "ASSET_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }
}
