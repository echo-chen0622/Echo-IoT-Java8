package org.echoiot.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.echoiot.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.common.transport.util.SslUtil;
import org.echoiot.server.transport.lwm2m.secure.credentials.LwM2MClientCredentials;
import org.echoiot.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.echoiot.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.echoiot.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mTypeServer;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    @NotNull
    private final TbLwM2MDtlsSessionStore sessionStorage;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2mCredentialsSecurityInfoValidator securityInfoValidator;
    @NotNull
    private final TbMainSecurityStore securityStore;

    private StaticNewAdvancedCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @NotNull
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize the LwM2M certificate verifier", e);
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
                        if (staticCertificateVerifier != null) {
                            HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                            if (exception == null) {
                                try {
                                    String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                                    if (StringUtils.isNotEmpty(endpoint)) {
                                        securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, LwM2mTypeServer.CLIENT);
                                    }
                                } catch (LwM2MAuthException e) {
                                    log.trace("Certificate trust validation failed.", e);
                                }
                            } else {
                                log.trace("Certificate trust validation failed.", exception);
                            }
                        }
                        // if not trust or cert trust securityInfo == null
                        @NotNull String strCert = SslUtil.getCertificateString(cert);
                        String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                        if (securityInfo == null || securityInfo.getMsg() == null) {
                            try {
                                securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(sha3Hash, LwM2mTypeServer.CLIENT);
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed find security info: {}", sha3Hash, e);
                            }
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                            @Nullable LwM2MClientCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MClientCredentials.class);
                            if (!credentials.getClient().getSecurityConfigClientMode().equals(LwM2MSecurityMode.X509)) {
                                continue;
                            }
                            X509ClientCredential config = (X509ClientCredential) credentials.getClient();
                            String certBody = config.getCert();
                            String endpoint = config.getEndpoint();
                            if (StringUtils.isBlank(certBody) || strCert.equals(certBody)) {
                                x509CredentialsFound = true;
                                DeviceProfile deviceProfile = msg.getDeviceProfile();
                                if (msg.hasDeviceInfo() && deviceProfile != null) {
                                    sessionStorage.put(endpoint, new TbX509DtlsSessionInfo(cert.getSubjectX500Principal().getName(), msg));
                                    try {
                                        securityStore.putX509(securityInfo);
                                    } catch (NonUniqueSecurityInfoException e) {
                                        log.trace("Failed to add security info: {}", securityInfo, e);
                                    }
                                    break;
                                }
                            } else {
                                log.trace("[{}][{}] Certificate mismatch. Expected: {}, Actual: {}", endpoint, sha3Hash, strCert, certBody);
                            }
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.error(e.getMessage(), e);
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
