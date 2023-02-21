package org.echoiot.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.transport.config.ssl.SslCredentials;
import org.echoiot.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService;
import org.echoiot.server.transport.lwm2m.server.store.TbSecurityStore;
import org.echoiot.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.server.cache.ota.OtaPackageDataCache;
import org.echoiot.server.queue.util.AfterStartUp;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.echoiot.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.echoiot.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PreDestroy;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.echoiot.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mTransportService implements LwM2MTransportService {

    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};

    @NotNull
    private final LwM2mTransportContext context;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final OtaPackageDataCache otaPackageDataCache;
    @NotNull
    private final DefaultLwM2mUplinkMsgHandler handler;
    @NotNull
    private final CaliforniumRegistrationStore registrationStore;
    @NotNull
    private final TbSecurityStore securityStore;
    @NotNull
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    @NotNull
    private final TbLwM2MAuthorizer authorizer;
    @NotNull
    private final LwM2mVersionedModelProvider modelProvider;

    private LeshanServer server;

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void init() {
        this.server = getLhServer();
        /*
         * Add a resource to the server.
         * CoapResource ->
         * path = FW_PACKAGE or SW_PACKAGE
         * nameFile = "BC68JAR01A09_TO_BC68JAR01A10.bin"
         * "coap://host:port/{path}/{token}/{nameFile}"
         */
        @NotNull LwM2mTransportCoapResource otaCoapResource = new LwM2mTransportCoapResource(otaPackageDataCache, DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE);
        this.server.coap().getServer().add(otaCoapResource);
        this.context.setServer(server);
        this.startLhServer();
    }

    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        @NotNull LwM2mServerListener lhServerCertListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.server.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.server.getObservationService().addListener(lhServerCertListener.observationListener);
        this.server.getSendService().addListener(lhServerCertListener.sendListener);
        log.info("Started LwM2M transport server.");
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("Stopping LwM2M transport server!");
            server.destroy();
            log.info("LwM2M transport server stopped!");
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport server!", e);
        }
    }

    private LeshanServer getLhServer() {
        @NotNull LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(config.getHost(), config.getPort());
        builder.setLocalSecureAddress(config.getSecureHost(), config.getSecurePort());
        builder.setDecoder(new DefaultLwM2mDecoder());
        /* Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mEncoder(LwM2mValueConverterImpl.getInstance()));

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        /* Define model provider (Create Models )*/
        builder.setObjectModelProvider(modelProvider);

        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);

        /* Create DTLS Config */
        @NotNull DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        dtlsConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, config.isRecommendedSupportedGroups());
        dtlsConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, config.isRecommendedCiphers());
        dtlsConfig.set(DTLS_RETRANSMISSION_TIMEOUT, config.getDtlsRetransmissionTimeout(), MILLISECONDS);
        dtlsConfig.set(DTLS_ROLE, SERVER_ONLY);

        /*  Create credentials */
        this.setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Create LWM2M server */
        return builder.build();
    }

    private void setServerWithCredentials(@NotNull LeshanServerBuilder builder, @NotNull DtlsConnectorConfig.Builder dtlsConfig) {
        if (this.config.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.config.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            builder.setAuthorizer(authorizer);
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for LWM2MServer");
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return DataConstants.LWM2M_TRANSPORT_NAME;
    }

}
