package org.echoiot.rule.engine.sms;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.sms.config.SmsProviderConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbSendSmsNodeConfiguration implements NodeConfiguration {

    private String numbersToTemplate;
    private String smsMessageTemplate;
    private boolean useSystemSmsSettings;
    private SmsProviderConfiguration smsProviderConfiguration;

    @NotNull
    @Override
    public NodeConfiguration defaultConfiguration() {
        @NotNull TbSendSmsNodeConfiguration configuration = new TbSendSmsNodeConfiguration();
        configuration.numbersToTemplate = "${userPhone}";
        configuration.smsMessageTemplate = "Device ${deviceName} has high temperature ${temp}";
        configuration.setUseSystemSmsSettings(true);
        return configuration;
    }
}
