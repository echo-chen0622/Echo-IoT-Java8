package org.thingsboard.edge.exception;

public class EdgeConnectionException extends RuntimeException {

    private static final long serialVersionUID = -4372754681230555723L;

    public EdgeConnectionException(String message) {
        super(message);
    }

    public EdgeConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
