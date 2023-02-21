package org.echoiot.server.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.sms.SmsSender;
import org.echoiot.rule.engine.api.sms.exception.SmsParseException;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractSmsSender implements SmsSender {

    protected static final Pattern E_164_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private static final int MAX_SMS_MESSAGE_LENGTH = 1600;
    private static final int MAX_SMS_SEGMENT_LENGTH = 70;

    @NotNull
    protected String validatePhoneNumber(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format.");
        }
        return phoneNumber;
    }

    @NotNull
    protected String prepareMessage(String message) {
        message = message.replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n");
        if (message.length() > MAX_SMS_MESSAGE_LENGTH) {
            log.warn("SMS message exceeds maximum symbols length and will be truncated");
            message = message.substring(0, MAX_SMS_MESSAGE_LENGTH);
        }
        return message;
    }

    protected int countMessageSegments(@NotNull String message) {
        return (int)Math.ceil((double) message.length() / (double) MAX_SMS_SEGMENT_LENGTH);
    }

}
