package org.echoiot.server.service.lwm2m;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.echoiot.server.common.transport.config.ssl.SslCredentials;
import org.echoiot.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${transport.lwm2m.enabled:false}'=='true'")
public class LwM2MServiceImpl implements LwM2MService {

    @NotNull
    private final LwM2MTransportServerConfig serverConfig;
    @NotNull
    private final Optional<LwM2MTransportBootstrapConfig> bootstrapConfig;

    @Nullable
    @Override
    public LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer) {
        @Nullable LwM2MSecureServerConfig bsServerConfig = bootstrapServer ? bootstrapConfig.orElse(null) : serverConfig;
        if (bsServerConfig!= null) {
            @NotNull LwM2MServerSecurityConfigDefault result = getServerSecurityConfig(bsServerConfig);
            result.setBootstrapServerIs(bootstrapServer);
            return result;
        }
        else {
            return  null;
        }
    }

    @NotNull
    private LwM2MServerSecurityConfigDefault getServerSecurityConfig(@NotNull LwM2MSecureServerConfig bsServerConfig) {
        @NotNull LwM2MServerSecurityConfigDefault bsServ = new LwM2MServerSecurityConfigDefault();
        bsServ.setShortServerId(bsServerConfig.getId());
        bsServ.setHost(bsServerConfig.getHost());
        bsServ.setPort(bsServerConfig.getPort());
        bsServ.setSecurityHost(bsServerConfig.getSecureHost());
        bsServ.setSecurityPort(bsServerConfig.getSecurePort());
        @Nullable byte[] publicKeyBase64 = getPublicKey(bsServerConfig);
        if (publicKeyBase64 == null) {
            bsServ.setServerPublicKey("");
        } else {
            bsServ.setServerPublicKey(Base64.encodeBase64String(publicKeyBase64));
        }
        @Nullable byte[] certificateBase64 = getCertificate(bsServerConfig);
        if (certificateBase64 == null) {
            bsServ.setServerCertificate("");
        } else {
            bsServ.setServerCertificate(Base64.encodeBase64String(certificateBase64));
        }
        return bsServ;
    }

    private byte[] getPublicKey(@NotNull LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getPublicKey().getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch public key from key store!", e);
        }
        return null;
    }

    private byte[] getCertificate(@NotNull LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getCertificateChain()[0].getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch certificate from key store!", e);
        }
        return null;
    }
}
