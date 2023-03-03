package org.echoiot.server.dao.exception;


/**
 * 参数错误异常
 *
 * @author Echo
 */
public class IncorrectParameterException extends RuntimeException {

    private static final long serialVersionUID = 601995650578985289L;

    public IncorrectParameterException(String message) {
        super(message);
    }

    public IncorrectParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
