package org.thingsboard.server.service.sms;

import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.thingsboard.server.service.sms.aws.AwsSmsSender;
import org.thingsboard.server.service.sms.smpp.SmppSmsSender;
import org.thingsboard.server.service.sms.twilio.TwilioSmsSender;

@Component
public class DefaultSmsSenderFactory implements SmsSenderFactory {

    @Override
    public SmsSender createSmsSender(SmsProviderConfiguration config) {
        switch (config.getType()) {
            case AWS_SNS:
                return new AwsSmsSender((AwsSnsSmsProviderConfiguration)config);
            case TWILIO:
                return new TwilioSmsSender((TwilioSmsProviderConfiguration)config);
            case SMPP:
                return new SmppSmsSender((SmppSmsProviderConfiguration) config);
            default:
                throw new RuntimeException("Unknown SMS provider type " + config.getType());
        }
    }

}
