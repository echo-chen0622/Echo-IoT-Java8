package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@Data
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class OAuth2ParamsInfo {

    @ApiModelProperty(value = "List of configured domains where OAuth2 platform will redirect a user after successful " +
            "authentication. Cannot be empty. There have to be only one domain with specific name with scheme type 'MIXED'. " +
            "Configured domains with the same name must have different scheme types", required = true)
    private List<OAuth2DomainInfo> domainInfos;
    @ApiModelProperty(value = "Mobile applications settings. Application package name must be unique within the list", required = true)
    private List<OAuth2MobileInfo> mobileInfos;
    @ApiModelProperty(value = "List of OAuth2 provider settings. Cannot be empty", required = true)
    private List<OAuth2RegistrationInfo> clientRegistrations;

}
