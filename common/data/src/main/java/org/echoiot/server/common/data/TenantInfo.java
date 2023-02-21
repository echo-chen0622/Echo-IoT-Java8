package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

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

    public TenantInfo(@NotNull Tenant tenant, String tenantProfileName) {
        super(tenant);
        this.tenantProfileName = tenantProfileName;
    }

}
