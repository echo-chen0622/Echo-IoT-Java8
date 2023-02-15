package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class OAuth2ClientInfo {

    @ApiModelProperty(value = "OAuth2 client name", example = "GitHub")
    private String name;
    @ApiModelProperty(value = "Name of the icon, displayed on OAuth2 log in button", example = "github-logo")
    private String icon;
    @ApiModelProperty(value = "URI for OAuth2 log in. On HTTP GET request to this URI, it redirects to the OAuth2 provider page",
            example = "/oauth2/authorization/8352f191-2b4d-11ec-9ed1-cbf57c026ecc")
    private String url;

    public OAuth2ClientInfo(OAuth2ClientInfo oauth2ClientInfo) {
        this.name = oauth2ClientInfo.getName();
        this.icon = oauth2ClientInfo.getIcon();
        this.url = oauth2ClientInfo.getUrl();
    }

}
