package org.echoiot.rule.engine.mqtt.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.echoiot.common.util.AzureIotHubUtil;
import org.echoiot.rule.engine.credentials.CertPemCredentials;
import org.echoiot.rule.engine.credentials.CredentialsType;
import org.jetbrains.annotations.NotNull;

import java.security.Security;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureIotHubSasCredentials extends CertPemCredentials {
    private String sasKey;

    @NotNull
    @Override
    public SslContext initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            if (caCert == null || caCert.isEmpty()) {
                caCert = AzureIotHubUtil.getDefaultCaCert();
            }
            return SslContextBuilder.forClient()
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
        } catch (Exception e) {
            log.error("[{}] Creating TLS factory failed!", caCert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    @NotNull
    @Override
    public CredentialsType getType() {
        return CredentialsType.SAS;
    }

}
