package org.thingsboard.server.service.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ActivateUserRequest {

    @ApiModelProperty(position = 1, value = "The activate token to verify", example = "AAB254FF67D..")
    private String activateToken;
    @ApiModelProperty(position = 2, value = "The new password to set", example = "secret")
    private String password;
}
