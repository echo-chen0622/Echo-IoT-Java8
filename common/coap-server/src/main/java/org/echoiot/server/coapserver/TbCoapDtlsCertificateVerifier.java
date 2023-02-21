package org.echoiot.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.common.transport.util.SslUtil;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TbCoapDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    @NotNull
    private final TbCoapDtlsSessionInMemoryStorage tbCoapDtlsSessionInMemoryStorage;

    private TransportService transportService;
    private TbServiceInfoProvider serviceInfoProvider;
    private boolean skipValidityCheckForClientCert;

    public TbCoapDtlsCertificateVerifier(TransportService transportService, TbServiceInfoProvider serviceInfoProvider, long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout, boolean skipValidityCheckForClientCert) {
        this.transportService = transportService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.skipValidityCheckForClientCert = skipValidityCheckForClientCert;
        this.tbCoapDtlsSessionInMemoryStorage = new TbCoapDtlsSessionInMemoryStorage(dtlsSessionInactivityTimeout, dtlsSessionReportTimeout);
    }

    @NotNull
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Collections.singletonList(CertificateType.X_509);
    }

    @NotNull
    @Override
    public CertificateVerificationResult verifyCertificate(@NotNull ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, @NotNull CertificateMessage message) {
        try {
            CertPath certpath = message.getCertificateChain();
            @NotNull X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
            for (@NotNull X509Certificate cert : chain) {
                try {
                    if (!skipValidityCheckForClientCert) {
                        cert.checkValidity();
                    }

                    @NotNull String strCert = SslUtil.getCertificateString(cert);
                    String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                    @NotNull final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
                    @NotNull CountDownLatch latch = new CountDownLatch(1);
                    transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                            new TransportServiceCallback<>() {
                                @Override
                                public void onSuccess(@NotNull ValidateDeviceCredentialsResponse msg) {
                                    if (!StringUtils.isEmpty(msg.getCredentials())) {
                                        deviceCredentialsResponse[0] = msg;
                                    }
                                    latch.countDown();
                                }

                                @Override
                                public void onError(@NotNull Throwable e) {
                                    log.error(e.getMessage(), e);
                                    latch.countDown();
                                }
                            });
                    latch.await(10, TimeUnit.SECONDS);
                    ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                    if (msg != null && strCert.equals(msg.getCredentials())) {
                        DeviceProfile deviceProfile = msg.getDeviceProfile();
                        if (msg.hasDeviceInfo() && deviceProfile != null) {
                            tbCoapDtlsSessionInMemoryStorage.put(remotePeer, new TbCoapDtlsSessionInfo(msg, deviceProfile));
                        }
                        break;
                    }
                } catch (InterruptedException |
                        CertificateEncodingException |
                        CertificateExpiredException |
                        CertificateNotYetValidException e) {
                    log.error(e.getMessage(), e);
                    @NotNull AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
                    throw new HandshakeException("Certificate chain could not be validated", alert);
                }
            }
            return new CertificateVerificationResult(cid, certpath, null);
        } catch (HandshakeException e) {
            log.trace("Certificate validation failed!", e);
            return new CertificateVerificationResult(cid, e, null);
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
    }

    public ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> getTbCoapDtlsSessionsMap() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionsMap();
    }

    public void evictTimeoutSessions() {
        tbCoapDtlsSessionInMemoryStorage.evictTimeoutSessions();
    }

    public long getDtlsSessionReportTimeout() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionReportTimeout();
    }
}
