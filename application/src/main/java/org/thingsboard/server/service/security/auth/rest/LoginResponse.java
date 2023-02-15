package org.thingsboard.server.service.security.auth.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LoginResponse {

    @ApiModelProperty(position = 1, required = true, value = "JWT token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String token;

    @ApiModelProperty(position = 2, required = true, value = "Refresh token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String refreshToken;

}
