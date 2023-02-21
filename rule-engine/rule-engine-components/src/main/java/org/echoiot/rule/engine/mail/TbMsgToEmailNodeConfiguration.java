package org.echoiot.rule.engine.mail;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgToEmailNodeConfiguration implements NodeConfiguration {

    private String fromTemplate;
    private String toTemplate;
    private String ccTemplate;
    private String bccTemplate;
    private String subjectTemplate;
    private String bodyTemplate;
    private String isHtmlTemplate;
    private String mailBodyType;

    @NotNull
    @Override
    public TbMsgToEmailNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgToEmailNodeConfiguration configuration = new TbMsgToEmailNodeConfiguration();
        configuration.fromTemplate = "info@testmail.org";
        configuration.toTemplate = "${userEmail}";
        configuration.subjectTemplate = "Device ${deviceType} temperature high";
        configuration.bodyTemplate = "Device ${deviceName} has high temperature ${temp}";
        configuration.mailBodyType = "false";
        return configuration;
    }
}
