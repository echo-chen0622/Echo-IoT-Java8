package org.thingsboard.rule.engine.api.sms.exception;

public abstract class SmsException extends RuntimeException {

    public SmsException(String msg) {
        super(msg);
    }

    public SmsException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
