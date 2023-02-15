package org.thingsboard.server.service.telemetry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EntityNotFoundException extends Exception implements ToErrorResponseEntity {

    public EntityNotFoundException(String message) {
        super(message);
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.NOT_FOUND);
    }
}
