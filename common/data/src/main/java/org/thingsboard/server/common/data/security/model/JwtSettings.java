package org.thingsboard.server.common.data.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(value = "JWT Settings")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JwtSettings {

    /**
     * {@link JwtToken} will expire after this time.
     */
    @ApiModelProperty(position = 1, value = "The JWT will expire after seconds.", example = "9000")
    private Integer tokenExpirationTime;

    /**
     * {@link JwtToken} can be refreshed during this timeframe.
     */
    @ApiModelProperty(position = 2, value = "The JWT can be refreshed during seconds.", example = "604800")
    private Integer refreshTokenExpTime;

    /**
     * Token issuer.
     */
    @ApiModelProperty(position = 3, value = "The JWT issuer.", example = "thingsboard.io")
    private String tokenIssuer;

    /**
     * Key is used to sign {@link JwtToken}.
     * Base64 encoded
     */
    @ApiModelProperty(position = 4, value = "The JWT key is used to sing token. Base64 encoded.", example = "cTU4WnNqemI2aU5wbWVjdm1vYXRzanhjNHRUcXliMjE=")
    private String tokenSigningKey;

}
