package org.thingsboard.rule.engine.api.sms;

import org.thingsboard.rule.engine.api.sms.exception.SmsException;

public interface SmsSender {

    int sendSms(String numberTo, String message) throws SmsException;

    void destroy();

}
