package org.echoiot.rule.engine.mail;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbSendEmailNodeConfiguration implements NodeConfiguration {

    private boolean useSystemSmtpSettings;
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private String smtpProtocol;
    private int timeout;
    private boolean enableTls;
    private String tlsVersion;
    private boolean enableProxy;
    private String proxyHost;
    private String proxyPort;
    private String proxyUser;
    private String proxyPassword;

    @NotNull
    @Override
    public TbSendEmailNodeConfiguration defaultConfiguration() {
        @NotNull TbSendEmailNodeConfiguration configuration = new TbSendEmailNodeConfiguration();
        configuration.setUseSystemSmtpSettings(true);
        configuration.setSmtpHost("localhost");
        configuration.setSmtpProtocol("smtp");
        configuration.setSmtpPort(25);
        configuration.setTimeout(10000);
        configuration.setEnableTls(false);
        configuration.setTlsVersion("TLSv1.2");
        configuration.setEnableProxy(false);
        return configuration;
    }
}
