package org.echoiot.server.service.security.auth.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LoginRequest {

    private final String username;

    private final String password;

    @JsonCreator
    public LoginRequest(@JsonProperty("username") String username, @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    @ApiModelProperty(position = 1, required = true, value = "User email", example = "tenant@echoiot.org")
    public String getUsername() {
        return username;
    }

    @ApiModelProperty(position = 2, required = true, value = "User password", example = "tenant")
    public String getPassword() {
        return password;
    }
}
