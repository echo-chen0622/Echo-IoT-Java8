package org.echoiot.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@EqualsAndHashCode
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel
public class OAuth2DomainInfo {
    @ApiModelProperty(value = "Domain scheme. Mixed scheme means than both HTTP and HTTPS are going to be used", required = true)
    private SchemeType scheme;
    @ApiModelProperty(value = "Domain name. Cannot be empty", required = true)
    private String name;
}
