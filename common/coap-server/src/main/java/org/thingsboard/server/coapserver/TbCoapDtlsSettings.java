package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.elements.config.CertificateAuthenticationMode.WANTED;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;

@Slf4j
@ConditionalOnProperty(prefix = "transport.coap.dtls", value = "enabled", havingValue = "true", matchIfMissing = false)
@Component
public class TbCoapDtlsSettings {

    @Value("${transport.coap.dtls.bind_address}")
    private String host;

    @Value("${transport.coap.dtls.bind_port}")
    private Integer port;

    @Value("${transport.coap.dtls.retransmission_timeout:9000}")
    private int dtlsRetransmissionTimeout;

    @Bean
    @ConfigurationProperties(prefix = "transport.coap.dtls.credentials")
    public SslCredentialsConfig coapDtlsCredentials() {
        return new SslCredentialsConfig("COAP DTLS Credentials", false);
    }

    @Autowired
    @Qualifier("coapDtlsCredentials")
    private SslCredentialsConfig coapDtlsCredentialsConfig;

    @Value("${transport.coap.dtls.x509.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Value("${transport.coap.dtls.x509.dtls_session_inactivity_timeout:86400000}")
    private long dtlsSessionInactivityTimeout;

    @Value("${transport.coap.dtls.x509.dtls_session_report_timeout:1800000}")
    private long dtlsSessionReportTimeout;

    @Autowired
    private TransportService transportService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    public DtlsConnectorConfig dtlsConnectorConfig(Configuration configuration) throws UnknownHostException {
        DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder(configuration);
        configBuilder.setAddress(getInetSocketAddress());
        SslCredentials sslCredentials = this.coapDtlsCredentialsConfig.getCredentials();
        SslContextUtil.Credentials serverCredentials =
                new SslContextUtil.Credentials(sslCredentials.getPrivateKey(), null, sslCredentials.getCertificateChain());
        configBuilder.set(DTLS_CLIENT_AUTHENTICATION_MODE, WANTED);
        configBuilder.set(DTLS_RETRANSMISSION_TIMEOUT, dtlsRetransmissionTimeout, MILLISECONDS);
        configBuilder.set(DTLS_ROLE, SERVER_ONLY);
        configBuilder.setAdvancedCertificateVerifier(
                new TbCoapDtlsCertificateVerifier(
                        transportService,
                        serviceInfoProvider,
                        dtlsSessionInactivityTimeout,
                        dtlsSessionReportTimeout,
                        skipValidityCheckForClientCert
                )
        );
        configBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                Collections.singletonList(CertificateType.X_509)));
        return configBuilder.build();
    }

    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

}
