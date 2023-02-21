package org.echoiot.server.service.telemetry.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Created by Echo on 21.02.17.
 */
public class UnauthorizedException extends Exception implements ToErrorResponseEntity {

    public UnauthorizedException(String message) {
        super(message);
    }

    @NotNull
    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
