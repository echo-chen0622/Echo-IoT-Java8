package org.echoiot.server.exception;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

@ApiModel
public class EchoiotCredentialsExpiredResponse extends EchoiotErrorResponse {

    private final String resetToken;

    protected EchoiotCredentialsExpiredResponse(String message, String resetToken) {
        super(message, EchoiotErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        this.resetToken = resetToken;
    }

    @NotNull
    public static EchoiotCredentialsExpiredResponse of(final String message, final String resetToken) {
        return new EchoiotCredentialsExpiredResponse(message, resetToken);
    }

    @ApiModelProperty(position = 5, value = "Password reset token", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getResetToken() {
        return resetToken;
    }
}
