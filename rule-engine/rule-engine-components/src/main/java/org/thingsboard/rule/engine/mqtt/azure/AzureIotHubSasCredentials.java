package org.thingsboard.rule.engine.mqtt.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureIotHubSasCredentials extends CertPemCredentials {
    private String sasKey;

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

    @Override
    public CredentialsType getType() {
        return CredentialsType.SAS;
    }

}
