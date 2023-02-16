package org.echoiot.server.common.data.exception;

public class EchoiotException extends Exception {

    private static final long serialVersionUID = 1L;

    private EchoiotErrorCode errorCode;

    public EchoiotException() {
        super();
    }

    public EchoiotException(EchoiotErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public EchoiotException(String message, EchoiotErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public EchoiotException(String message, Throwable cause, EchoiotErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public EchoiotException(Throwable cause, EchoiotErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public EchoiotErrorCode getErrorCode() {
        return errorCode;
    }

}
