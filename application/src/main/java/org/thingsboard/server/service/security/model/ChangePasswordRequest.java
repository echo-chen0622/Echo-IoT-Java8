package org.thingsboard.server.service.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ChangePasswordRequest {

    @ApiModelProperty(position = 1, value = "The old password", example = "OldPassword")
    private String currentPassword;
    @ApiModelProperty(position = 1, value = "The new password", example = "NewPassword")
    private String newPassword;

}
