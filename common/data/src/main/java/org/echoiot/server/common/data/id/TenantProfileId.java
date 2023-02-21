package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TenantProfileId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public TenantProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static TenantProfileId fromString(@NotNull String tenantProfileId) {
        return new TenantProfileId(UUID.fromString(tenantProfileId));
    }

    @NotNull
    @ApiModelProperty(position = 2, required = true, value = "string", example = "TENANT_PROFILE", allowableValues = "TENANT_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT_PROFILE;
    }
}
