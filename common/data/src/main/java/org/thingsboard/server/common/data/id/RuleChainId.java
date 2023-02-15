package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class RuleChainId extends UUIDBased implements EntityId {

    @JsonCreator
    public RuleChainId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "RULE_CHAIN", allowableValues = "RULE_CHAIN")
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }
}
