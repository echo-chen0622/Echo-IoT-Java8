package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RuleNodeId extends UUIDBased implements EntityId {

    @JsonCreator
    public RuleNodeId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "RULE_NODE", allowableValues = "RULE_NODE")
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_NODE;
    }
}
