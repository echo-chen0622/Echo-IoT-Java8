package org.echoiot.script.api;

import lombok.Getter;

import java.util.UUID;

public class TbScriptException extends RuntimeException {
    private static final long serialVersionUID = -1958193538782818284L;

    public static enum ErrorCode {COMPILATION, TIMEOUT, RUNTIME, OTHER}

    @Getter
    private final UUID scriptId;
    @Getter
    private final ErrorCode errorCode;
    @Getter
    private final String body;

    public TbScriptException(UUID scriptId, ErrorCode errorCode, String body, Exception cause) {
        super(cause);
        this.scriptId = scriptId;
        this.errorCode = errorCode;
        this.body = body;
    }
}
