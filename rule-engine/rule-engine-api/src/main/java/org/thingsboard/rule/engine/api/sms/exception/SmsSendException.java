package org.thingsboard.rule.engine.api.sms.exception;

public class SmsSendException extends SmsException {

    public SmsSendException(String msg) {
        super(msg);
    }

    public SmsSendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
