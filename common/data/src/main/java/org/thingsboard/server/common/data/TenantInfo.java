package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@ApiModel
@Data
public class TenantInfo extends Tenant {
    @ApiModelProperty(position = 15, value = "Tenant Profile name", example = "Default")
    private String tenantProfileName;

    public TenantInfo() {
        super();
    }

    public TenantInfo(TenantId tenantId) {
        super(tenantId);
    }

    public TenantInfo(Tenant tenant, String tenantProfileName) {
        super(tenant);
        this.tenantProfileName = tenantProfileName;
    }

}
