package org.echoiot.server.exception;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import java.util.Date;

@ApiModel
public class EchoiotErrorResponse {
    // HTTP Response Status Code
    private final HttpStatus status;

    // General Error message
    private final String message;

    // Error code
    private final EchoiotErrorCode errorCode;

    @NotNull
    private final Date timestamp;

    protected EchoiotErrorResponse(final String message, final EchoiotErrorCode errorCode, HttpStatus status) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
        this.timestamp = new java.util.Date();
    }

    @NotNull
    public static EchoiotErrorResponse of(final String message, final EchoiotErrorCode errorCode, HttpStatus status) {
        return new EchoiotErrorResponse(message, errorCode, status);
    }

    @NotNull
    @ApiModelProperty(position = 1, value = "HTTP Response Status Code", example = "401", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Integer getStatus() {
        return status.value();
    }

    @ApiModelProperty(position = 2, value = "Error message", example = "Authentication failed", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getMessage() {
        return message;
    }

    @ApiModelProperty(position = 3, value = "Platform error code:" +
            "\n* `2` - General error (HTTP: 500 - Internal Server Error)" +
            "\n\n* `10` - Authentication failed (HTTP: 401 - Unauthorized)" +
            "\n\n* `11` - JWT token expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `15` - Credentials expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `20` - Permission denied (HTTP: 403 - Forbidden)" +
            "\n\n* `30` - Invalid arguments (HTTP: 400 - Bad Request)" +
            "\n\n* `31` - Bad request params (HTTP: 400 - Bad Request)" +
            "\n\n* `32` - Item not found (HTTP: 404 - Not Found)" +
            "\n\n* `33` - Too many requests (HTTP: 429 - Too Many Requests)" +
            "\n\n* `34` - Too many updates (Too many updates over Websocket session)" +
            "\n\n* `40` - Subscription violation (HTTP: 403 - Forbidden)",
            example = "10", dataType = "integer",
            accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public EchoiotErrorCode getErrorCode() {
        return errorCode;
    }

    @NotNull
    @ApiModelProperty(position = 4, value = "Timestamp", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Date getTimestamp() {
        return timestamp;
    }
}
