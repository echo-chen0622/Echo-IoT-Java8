package org.echoiot.server.transport.lwm2m.bootstrap.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.common.transport.util.SslUtil;
import org.echoiot.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.echoiot.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.echoiot.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.*;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.*;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@TbLwM2mBootstrapTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsBootstrapCertificateVerifier implements NewAdvancedCertificateVerifier {

    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2MBootstrapSecurityStore bsSecurityStore;

    private StaticNewAdvancedCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @NotNull
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @SuppressWarnings("deprecation")
    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("ailed to initialize the LwM2M certificate verifier", e);
        }
    }

    @NotNull
    @Override
    public CertificateVerificationResult verifyCertificate(@NotNull ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer,
                                                           boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath,
                                                           @NotNull CertificateMessage message) {
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer, and use TbLwM2MAuthorizer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            try {
                boolean x509CredentialsFound = false;
                @NotNull X509Certificate[] chain = certChain.getCertificates().toArray(new X509Certificate[0]);
                for (@NotNull X509Certificate cert : chain) {
                    try {
                        if (!skipValidityCheckForClientCert) {
                            cert.checkValidity();
                        }
                        @Nullable TbLwM2MSecurityInfo securityInfo = null;
                        // verify if trust
                        if (staticCertificateVerifier != null) {
                            HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                            if (exception == null) {
                                String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                                if (StringUtils.isNotEmpty(endpoint)) {
                                    securityInfo = bsSecurityStore.getX509ByEndpoint(endpoint);
                                }
                            } else {
                                log.trace("Certificate validation failed.", exception);
                            }
                        }
                        // if not trust or cert trust securityInfo == null
                        if (securityInfo == null || securityInfo.getMsg() == null) {
                            @NotNull String strCert = SslUtil.getCertificateString(cert);
                            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                            try {
                                securityInfo = bsSecurityStore.getX509ByEndpoint(sha3Hash);
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed to find security info: [{}]", sha3Hash, e);
                            }
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                            x509CredentialsFound = true;
                            break;
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.trace("Failed to find security info: [{}]", cert.getSubjectX500Principal().getName(), e);
                    }
                }
                if (!x509CredentialsFound) {
                    @NotNull AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.INTERNAL_ERROR);
                    throw new HandshakeException("x509 verification not enabled!", alert);
                }
                return new CertificateVerificationResult(cid, certChain, null);
            } catch (HandshakeException e) {
                log.trace("Certificate validation failed!", e);
                return new CertificateVerificationResult(cid, e, null);
            }
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {

    }
}
