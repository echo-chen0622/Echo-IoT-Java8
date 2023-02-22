package org.echoiot.server.common.data;

import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EntitySubtype {

    private static final long serialVersionUID = 8057240243059922101L;

    private TenantId tenantId;
    private EntityType entityType;
    private String type;

    public EntitySubtype() {
        super();
    }

    public EntitySubtype(TenantId tenantId, EntityType entityType, String type) {
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.type = type;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntitySubtype that = (EntitySubtype) o;

        if (!Objects.equals(tenantId, that.tenantId)) return false;
        if (entityType != that.entityType) return false;
        return Objects.equals(type, that.type);

    }

    @Override
    public int hashCode() {
        int result = tenantId != null ? tenantId.hashCode() : 0;
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String sb = "EntitySubtype{" + "tenantId=" + tenantId + ", entityType=" + entityType + ", type='" + type + '\'' + '}';
        return sb;
    }

}
