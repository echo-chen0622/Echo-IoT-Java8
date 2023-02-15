package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public final class TenantId extends UUIDBased implements EntityId {

    @JsonIgnore
    static final ConcurrentReferenceHashMap<UUID, TenantId> tenants = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    @JsonIgnore
    public static final TenantId SYS_TENANT_ID = TenantId.fromUUID(EntityId.NULL_UUID);

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public static TenantId fromUUID(@JsonProperty("id") UUID id) {
        return tenants.computeIfAbsent(id, TenantId::new);
    }

    //default constructor is still available due to possible usage in extensions
    public TenantId(UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "TENANT", allowableValues = "TENANT")
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }
}
