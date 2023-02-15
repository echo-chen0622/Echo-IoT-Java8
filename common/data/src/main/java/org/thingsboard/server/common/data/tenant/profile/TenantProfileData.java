package org.thingsboard.server.common.data.tenant.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel
@Data
public class TenantProfileData {

    @ApiModelProperty(position = 1, value = "Complex JSON object that contains profile settings: max devices, max assets, rate limits, etc.")
    private TenantProfileConfiguration configuration;

    @ApiModelProperty(position = 2, value = "JSON array of queue configuration per tenant profile")
    private List<TenantProfileQueueConfiguration> queueConfiguration;

}
