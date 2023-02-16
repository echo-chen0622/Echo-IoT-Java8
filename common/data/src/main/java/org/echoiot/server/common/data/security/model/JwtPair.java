package org.echoiot.server.common.data.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.echoiot.server.common.data.security.Authority;

@ApiModel(value = "JWT Pair")
@Data
@NoArgsConstructor
public class JwtPair {

    @ApiModelProperty(position = 1, value = "The JWT Access Token. Used to perform API calls.", example = "AAB254FF67D..")
    private String token;
    @ApiModelProperty(position = 1, value = "The JWT Refresh Token. Used to get new JWT Access Token if old one has expired.", example = "AAB254FF67D..")
    private String refreshToken;

    private Authority scope;

    public JwtPair(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

}
