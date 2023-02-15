package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2MServerSecurityConfigDefault extends LwM2MServerSecurityConfig {
    @ApiModelProperty(position = 5, value = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected String securityHost;
    @ApiModelProperty(position = 6, value = "Port for 'Security' mode (DTLS): Lwm2m Server or Bootstrap Server", example = "5686 or 5688", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected Integer securityPort;
}
