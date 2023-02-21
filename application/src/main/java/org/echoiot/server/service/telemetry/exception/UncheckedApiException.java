package org.echoiot.server.service.telemetry.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public class UncheckedApiException extends RuntimeException implements ToErrorResponseEntity {

    @NotNull
    private final ToErrorResponseEntity cause;

    public <T extends Exception & ToErrorResponseEntity> UncheckedApiException(@NotNull T cause) {
        super(cause.getMessage(), Objects.requireNonNull(cause));
        this.cause = cause;
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return cause.toErrorResponseEntity();
    }
}
