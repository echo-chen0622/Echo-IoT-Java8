package org.thingsboard.rule.engine.api.sms.exception;

public class SmsParseException extends SmsException {

    public SmsParseException(String msg) {
        super(msg);
    }

    public SmsParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
