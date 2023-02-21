package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.TenantInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantInfoEntity extends AbstractTenantEntity<TenantInfo> {

    public static final Map<String,String> tenantInfoColumnMap = new HashMap<>();
    static {
        tenantInfoColumnMap.put("tenantProfileName", "p.name");
    }

    private String tenantProfileName;

    public TenantInfoEntity() {
        super();
    }

    public TenantInfoEntity(@NotNull TenantEntity tenantEntity, String tenantProfileName) {
        super(tenantEntity);
        this.tenantProfileName = tenantProfileName;
    }

    @NotNull
    @Override
    public TenantInfo toData() {
        return new TenantInfo(super.toTenant(), this.tenantProfileName);
    }
}
