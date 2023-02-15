package org.thingsboard.server.exception;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@ApiModel
public class ThingsboardCredentialsExpiredResponse extends ThingsboardErrorResponse {

    private final String resetToken;

    protected ThingsboardCredentialsExpiredResponse(String message, String resetToken) {
        super(message, ThingsboardErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        this.resetToken = resetToken;
    }

    public static ThingsboardCredentialsExpiredResponse of(final String message, final String resetToken) {
        return new ThingsboardCredentialsExpiredResponse(message, resetToken);
    }

    @ApiModelProperty(position = 5, value = "Password reset token", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getResetToken() {
        return resetToken;
    }
}
