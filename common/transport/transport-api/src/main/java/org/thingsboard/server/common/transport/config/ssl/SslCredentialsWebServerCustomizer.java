package org.thingsboard.server.common.transport.config.ssl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.security.KeyStore;

@Component
@ConditionalOnExpression("'${spring.main.web-environment:true}'=='true' && '${server.ssl.enabled:false}'=='true'")
public class SslCredentialsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Bean
    @ConfigurationProperties(prefix = "server.ssl.credentials")
    public SslCredentialsConfig httpServerSslCredentials() {
        return new SslCredentialsConfig("HTTP Server SSL Credentials", false);
    }

    @Autowired
    @Qualifier("httpServerSslCredentials")
    private SslCredentialsConfig httpServerSslCredentialsConfig;

    private final ServerProperties serverProperties;

    public SslCredentialsWebServerCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        SslCredentials sslCredentials = this.httpServerSslCredentialsConfig.getCredentials();
        Ssl ssl = serverProperties.getSsl();
        ssl.setKeyAlias(sslCredentials.getKeyAlias());
        ssl.setKeyPassword(sslCredentials.getKeyPassword());
        factory.setSsl(ssl);
        factory.setSslStoreProvider(new SslStoreProvider() {
            @Override
            public KeyStore getKeyStore() {
                return sslCredentials.getKeyStore();
            }

            @Override
            public KeyStore getTrustStore() {
                return null;
            }
        });
    }
}
