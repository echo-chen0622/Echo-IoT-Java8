package org.echoiot.server.service.telemetry.sub;

import org.jetbrains.annotations.NotNull;

public enum SubscriptionErrorCode {

    NO_ERROR(0), INTERNAL_ERROR(1, "Internal Server error!"), BAD_REQUEST(2, "Bad request"), UNAUTHORIZED(3, "Unauthorized");

    private final int code;
    private final String defaultMsg;

    SubscriptionErrorCode(int code) {
        this(code, null);
    }

    SubscriptionErrorCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }

    @NotNull
    public static SubscriptionErrorCode forCode(int code) {
        for (@NotNull SubscriptionErrorCode errorCode : SubscriptionErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Invalid error code: " + code);
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMsg() {
        return defaultMsg;
    }
}
