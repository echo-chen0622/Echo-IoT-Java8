package org.thingsboard.server.service.telemetry.exception;

import org.springframework.http.ResponseEntity;

import java.util.Objects;

public class UncheckedApiException extends RuntimeException implements ToErrorResponseEntity {

    private final ToErrorResponseEntity cause;

    public <T extends Exception & ToErrorResponseEntity> UncheckedApiException(T cause) {
        super(cause.getMessage(), Objects.requireNonNull(cause));
        this.cause = cause;
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return cause.toErrorResponseEntity();
    }
}
