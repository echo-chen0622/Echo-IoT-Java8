package org.echoiot.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@EqualsAndHashCode
@Data
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class OAuth2Info {
    @ApiModelProperty("Whether OAuth2 settings are enabled or not")
    private boolean enabled;
    @ApiModelProperty(value = "List of configured OAuth2 clients. Cannot contain null values", required = true)
    private List<OAuth2ParamsInfo> oauth2ParamsInfos;
}
