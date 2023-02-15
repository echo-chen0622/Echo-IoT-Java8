package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@ApiModel
public class AssetId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AssetId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AssetId fromString(String assetId) {
        return new AssetId(UUID.fromString(assetId));
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "ASSET", allowableValues = "ASSET")
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }
}
