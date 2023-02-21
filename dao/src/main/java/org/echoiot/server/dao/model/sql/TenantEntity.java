package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.TENANT_COLUMN_FAMILY_NAME)
public final class TenantEntity extends AbstractTenantEntity<Tenant> {

    public TenantEntity() {
        super();
    }

    public TenantEntity(@NotNull Tenant tenant) {
        super(tenant);
    }

    @Override
    public Tenant toData() {
        return super.toTenant();
    }
}
