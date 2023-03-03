package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

import java.util.UUID;

/**
 * 租户ID
 *
 * @author Echo
 */
public final class TenantId extends UUIDBased implements EntityId {

    /**
     * 租户缓存
     */
    @JsonIgnore
    static final ConcurrentReferenceHashMap<UUID, TenantId> tenants = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    /**
     * 系统租户ID
     */
    @JsonIgnore
    public static final TenantId SYS_TENANT_ID = TenantId.fromUUID(NULL_UUID);

    private static final long serialVersionUID = 1L;

    /**
     * 从UUID获取或创建租户ID
     *
     * @param id UUID
     *
     * @return 租户ID
     */
    @JsonCreator
    public static TenantId fromUUID(@JsonProperty("id") UUID id) {
        return tenants.computeIfAbsent(id, TenantId::new);
    }

    /**
     * 由于可能在扩展中使用，默认构造函数仍然可用
     */
    public TenantId(UUID id) {
        super(id);
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "TENANT", allowableValues = "TENANT")
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }
}
