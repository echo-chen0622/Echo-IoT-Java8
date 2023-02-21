package org.echoiot.server.transport.lwm2m.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.config.ssl.SslCredentials;
import org.echoiot.server.common.transport.config.ssl.SslCredentialsConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')  && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enabled:false}'=='true'")
public class LwM2MTransportBootstrapConfig implements LwM2MSecureServerConfig {

    @Getter
    @Value("${transport.lwm2m.bootstrap.id:}")
    private Integer id;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port:}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_address:}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_port:}")
    private Integer securePort;

    @NotNull
    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.bootstrap.security.credentials")
    public SslCredentialsConfig lwm2mBootstrapCredentials() {
        return new SslCredentialsConfig("LWM2M Bootstrap DTLS Credentials", false);
    }

    @Resource(name = "lwm2mBootstrapCredentials")
    private SslCredentialsConfig credentialsConfig;

    @Override
    public SslCredentials getSslCredentials() {
        return this.credentialsConfig.getCredentials();
    }
}
