package org.echoiot.server.service.telemetry.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class InvalidParametersException extends Exception implements ToErrorResponseEntity {

    public InvalidParametersException(String message) {
        super(message);
    }

    @NotNull
    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.BAD_REQUEST);
    }
}
