package org.thingsboard.rule.engine.mail;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

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

    @Override
    public TbMsgToEmailNodeConfiguration defaultConfiguration() {
        TbMsgToEmailNodeConfiguration configuration = new TbMsgToEmailNodeConfiguration();
        configuration.fromTemplate = "info@testmail.org";
        configuration.toTemplate = "${userEmail}";
        configuration.subjectTemplate = "Device ${deviceType} temperature high";
        configuration.bodyTemplate = "Device ${deviceName} has high temperature ${temp}";
        configuration.mailBodyType = "false";
        return configuration;
    }
}
