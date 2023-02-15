package org.thingsboard.server.service.telemetry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Created by ashvayka on 21.02.17.
 */
public class UnauthorizedException extends Exception implements ToErrorResponseEntity {

    public UnauthorizedException(String message) {
        super(message);
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
