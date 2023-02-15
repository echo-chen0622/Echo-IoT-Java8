package org.thingsboard.server.service.security.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ResetPasswordEmailRequest {

    @ApiModelProperty(position = 1, value = "The email of the user", example = "user@example.com")
    private String email;
}
