package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel
public class OAuth2MobileInfo {
    @ApiModelProperty(value = "Application package name. Cannot be empty", required = true)
    private String pkgName;
    @ApiModelProperty(value = "Application secret. The length must be at least 16 characters", required = true)
    private String appSecret;
}
