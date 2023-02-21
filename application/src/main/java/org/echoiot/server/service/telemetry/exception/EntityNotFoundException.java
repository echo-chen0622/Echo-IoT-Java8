package org.echoiot.server.service.telemetry.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EntityNotFoundException extends Exception implements ToErrorResponseEntity {

    public EntityNotFoundException(String message) {
        super(message);
    }

    @NotNull
    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.NOT_FOUND);
    }
}
