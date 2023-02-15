package org.thingsboard.server.common.data.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class SecuritySettings implements Serializable {

    private static final long serialVersionUID = -1307613974597312465L;

    @ApiModelProperty(position = 1, value = "The user password policy object." )
    private UserPasswordPolicy passwordPolicy;
    @ApiModelProperty(position = 2, value = "Maximum number of failed login attempts allowed before user account is locked." )
    private Integer maxFailedLoginAttempts;
    @ApiModelProperty(position = 3, value = "Email to use for notifications about locked users." )
    private String userLockoutNotificationEmail;
}
