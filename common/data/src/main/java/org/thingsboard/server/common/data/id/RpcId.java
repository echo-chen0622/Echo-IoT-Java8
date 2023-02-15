package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public final class RpcId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public RpcId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "RPC", allowableValues = "RPC")
    @Override
    public EntityType getEntityType() {
        return EntityType.RPC;
    }
}
