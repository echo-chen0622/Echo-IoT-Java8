package org.echoiot.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.device.credentials.lwm2m.*;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.DeviceCredentialsValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceCredentialsServiceImpl extends AbstractCachedEntityService<String, DeviceCredentials, DeviceCredentialsEvictEvent> implements DeviceCredentialsService {

    @Resource
    private DeviceCredentialsDao deviceCredentialsDao;

    @Resource
    private DataValidator<DeviceCredentials> credentialsValidator;

    @TransactionalEventListener(classes = DeviceCredentialsEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull DeviceCredentialsEvictEvent event) {
        cache.evict(event.getNewCedentialsId());
        if (StringUtils.isNotEmpty(event.getOldCredentialsId()) && !event.getNewCedentialsId().equals(event.getOldCredentialsId())) {
            cache.evict(event.getOldCredentialsId());
        }
    }

    @Override
    public DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, @NotNull DeviceId deviceId) {
        log.trace("Executing findDeviceCredentialsByDeviceId [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        return deviceCredentialsDao.findByDeviceId(tenantId, deviceId.getId());
    }

    @Override
    public DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId) {
        log.trace("Executing findDeviceCredentialsByCredentialsId [{}]", credentialsId);
        Validator.validateString(credentialsId, "Incorrect credentialsId " + credentialsId);
        return cache.getAndPutInTransaction(credentialsId,
                () -> deviceCredentialsDao.findByCredentialsId(TenantId.SYS_TENANT_ID, credentialsId),
                false);
    }

    @NotNull
    @Override
    public DeviceCredentials updateDeviceCredentials(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    @NotNull
    @Override
    public DeviceCredentials createDeviceCredentials(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    @NotNull
    private DeviceCredentials saveOrUpdate(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DataValidationException("Device credentials type should be specified");
        }
        formatCredentials(deviceCredentials);
        log.trace("Executing updateDeviceCredentials [{}]", deviceCredentials);
        credentialsValidator.validate(deviceCredentials, id -> tenantId);
        @Nullable DeviceCredentials oldDeviceCredentials = null;
        if (deviceCredentials.getDeviceId() != null) {
            oldDeviceCredentials = deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId());
        }
        try {
            var value = deviceCredentialsDao.saveAndFlush(tenantId, deviceCredentials);
            publishEvictEvent(new DeviceCredentialsEvictEvent(value.getCredentialsId(), oldDeviceCredentials != null ? oldDeviceCredentials.getCredentialsId() : null));
            return value;
        } catch (Exception t) {
            handleEvictEvent(new DeviceCredentialsEvictEvent(deviceCredentials.getCredentialsId(), oldDeviceCredentials != null ? oldDeviceCredentials.getCredentialsId() : null));
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && (e.getConstraintName().equalsIgnoreCase("device_credentials_id_unq_key") || e.getConstraintName().equalsIgnoreCase("device_credentials_device_id_unq_key"))) {
                throw new DataValidationException("Specified credentials are already registered!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public void formatCredentials(@NotNull DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case X509_CERTIFICATE:
                formatCertData(deviceCredentials);
                break;
            case MQTT_BASIC:
                formatSimpleMqttCredentials(deviceCredentials);
                break;
            case LWM2M_CREDENTIALS:
                formatAndValidateSimpleLwm2mCredentials(deviceCredentials);
                break;
        }
    }

    @Nullable
    @Override
    public JsonNode toCredentialsInfo(@NotNull DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                return JacksonUtil.valueToTree(deviceCredentials.getCredentialsId());
            case X509_CERTIFICATE:
                return JacksonUtil.valueToTree(deviceCredentials.getCredentialsValue());
        }
        return JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), JsonNode.class);
    }

    private void formatSimpleMqttCredentials(@NotNull DeviceCredentials deviceCredentials) {
        @Nullable BasicMqttCredentials mqttCredentials;
        try {
            mqttCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
            if (mqttCredentials == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for simple mqtt credentials!");
        }

        if (StringUtils.isEmpty(mqttCredentials.getClientId()) && StringUtils.isEmpty(mqttCredentials.getUserName())) {
            throw new DeviceCredentialsValidationException("Both mqtt client id and user name are empty!");
        }
        if (StringUtils.isNotEmpty(mqttCredentials.getClientId()) && StringUtils.isNotEmpty(mqttCredentials.getPassword()) && StringUtils.isEmpty(mqttCredentials.getUserName())) {
            throw new DeviceCredentialsValidationException("Password cannot be specified along with client id");
        }

        if (StringUtils.isEmpty(mqttCredentials.getClientId())) {
            deviceCredentials.setCredentialsId(mqttCredentials.getUserName());
        } else if (StringUtils.isEmpty(mqttCredentials.getUserName())) {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(mqttCredentials.getClientId()));
        } else {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash("|", mqttCredentials.getClientId(), mqttCredentials.getUserName()));
        }
        if (StringUtils.isNotEmpty(mqttCredentials.getPassword())) {
            mqttCredentials.setPassword(mqttCredentials.getPassword());
        }
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(mqttCredentials));
    }

    private void formatCertData(@NotNull DeviceCredentials deviceCredentials) {
        @NotNull String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        deviceCredentials.setCredentialsId(sha3Hash);
        deviceCredentials.setCredentialsValue(cert);
    }

    private void formatAndValidateSimpleLwm2mCredentials(@NotNull DeviceCredentials deviceCredentials) {
        @Nullable LwM2MDeviceCredentials lwM2MCredentials;
        try {
            lwM2MCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
            validateLwM2MDeviceCredentials(lwM2MCredentials);
        } catch (IllegalArgumentException e) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for LwM2M credentials!");
        }

        @Nullable String credentialsId = null;
        LwM2MClientCredential clientCredentials = lwM2MCredentials.getClient();
        switch (clientCredentials.getSecurityConfigClientMode()) {
            case NO_SEC:
            case RPK:
                deviceCredentials.setCredentialsValue(JacksonUtil.toString(lwM2MCredentials));
                credentialsId = clientCredentials.getEndpoint();
                break;
            case PSK:
                credentialsId = ((PSKClientCredential) clientCredentials).getIdentity();
                break;
            case X509:
                deviceCredentials.setCredentialsValue(JacksonUtil.toString(lwM2MCredentials));
                @NotNull X509ClientCredential x509ClientConfig = (X509ClientCredential) clientCredentials;
                if ((StringUtils.isNotBlank(x509ClientConfig.getCert()))) {
                    credentialsId = EncryptionUtil.getSha3Hash(x509ClientConfig.getCert());
                } else {
                    credentialsId = x509ClientConfig.getEndpoint();
                }
                break;
        }
        if (credentialsId == null) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for LwM2M credentials!");
        }
        deviceCredentials.setCredentialsId(credentialsId);
    }

    private void validateLwM2MDeviceCredentials(@NotNull LwM2MDeviceCredentials lwM2MCredentials) {
        if (lwM2MCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M credentials must be specified!");
        }

        LwM2MClientCredential clientCredentials = lwM2MCredentials.getClient();
        if (clientCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M client credentials must be specified!");
        }
        validateLwM2MClientCredentials(clientCredentials);

        LwM2MBootstrapClientCredentials bootstrapCredentials = lwM2MCredentials.getBootstrap();
        if (bootstrapCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M bootstrap credentials must be specified!");
        }

        LwM2MBootstrapClientCredential bootstrapServerCredentials = bootstrapCredentials.getBootstrapServer();
        if (bootstrapServerCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M bootstrap server credentials must be specified!");
        }
        validateServerCredentials(bootstrapServerCredentials, "Bootstrap server");

        LwM2MBootstrapClientCredential lwm2MBootstrapClientCredential = bootstrapCredentials.getLwm2mServer();
        if (lwm2MBootstrapClientCredential == null) {
            throw new DeviceCredentialsValidationException("LwM2M lwm2m server credentials must be specified!");
        }
        validateServerCredentials(lwm2MBootstrapClientCredential, "LwM2M server");
    }

    private void validateLwM2MClientCredentials(@NotNull LwM2MClientCredential clientCredentials) {
        if (StringUtils.isBlank(clientCredentials.getEndpoint())) {
            throw new DeviceCredentialsValidationException("LwM2M client endpoint must be specified!");
        }

        switch (clientCredentials.getSecurityConfigClientMode()) {
            case NO_SEC:
                break;
            case PSK:
                @NotNull PSKClientCredential pskCredentials = (PSKClientCredential) clientCredentials;
                if (StringUtils.isBlank(pskCredentials.getIdentity())) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK identity must be specified and must be an utf8 string!");
                }
                // SecurityMode.NO_SEC.toString() == "NO_SEC";
                if (pskCredentials.getIdentity().equals(SecurityMode.NO_SEC.toString())) {
                    throw new DeviceCredentialsValidationException("The PSK ID of the LwM2M client must not be '" + SecurityMode.NO_SEC + "'!");
                }

                String pskKey = pskCredentials.getKey();
                if (StringUtils.isBlank(pskKey)) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key must be specified!");
                }

                if (!pskKey.matches("-?[0-9a-fA-F]+")) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key must be random sequence in hex encoding!");
                }

                if (pskKey.length() % 32 != 0 || pskKey.length() > 128) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key length = " + pskKey.length() + ". Key must be HexDec format: 32, 64, 128 characters!");
                }

                break;
            case RPK:
                @NotNull RPKClientCredential rpkCredentials = (RPKClientCredential) clientCredentials;
                if (StringUtils.isBlank(rpkCredentials.getKey())) {
                    throw new DeviceCredentialsValidationException("LwM2M client RPK key must be specified!");
                }

                try {
                    @NotNull String pubkClient = EncryptionUtil.pubkTrimNewLines(rpkCredentials.getKey());
                    rpkCredentials.setKey(pubkClient);
                    SecurityUtil.publicKey.decode(rpkCredentials.getDecoded());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException("LwM2M client RPK key must be in standard [RFC7250] and support only EC algorithm and then encoded to Base64 format!");
                }
                break;
            case X509:
                @NotNull X509ClientCredential x509CCredentials = (X509ClientCredential) clientCredentials;
                if (StringUtils.isNotEmpty(x509CCredentials.getCert())) {
                    try {
                        @NotNull String certClient = EncryptionUtil.certTrimNewLines(x509CCredentials.getCert());
                        x509CCredentials.setCert(certClient);
                        SecurityUtil.certificate.decode(x509CCredentials.getDecoded());
                    } catch (Exception e) {
                        throw new DeviceCredentialsValidationException("LwM2M client X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                    }
                }
                break;
        }
    }

    private void validateServerCredentials(@NotNull LwM2MBootstrapClientCredential serverCredentials, String server) {
        switch (serverCredentials.getSecurityMode()) {
            case NO_SEC:
                break;
            case PSK:
                @NotNull PSKBootstrapClientCredential pskCredentials = (PSKBootstrapClientCredential) serverCredentials;
                if (StringUtils.isBlank(pskCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client PSK public key or id must be specified and must be an utf8 string!");
                }

                // SecurityMode.NO_SEC.toString() == "NO_SEC";
                if (pskCredentials.getClientPublicKeyOrId().equals(SecurityMode.NO_SEC.toString())) {
                    throw new DeviceCredentialsValidationException(server + " client PSK public key or id must not be '" + SecurityMode.NO_SEC + "'!");
                }

                String pskKey = pskCredentials.getClientSecretKey();
                if (StringUtils.isBlank(pskKey)) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key must be specified!");
                }

                if (!pskKey.matches("-?[0-9a-fA-F]+")) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key must be random sequence in hex encoding!");
                }

                if (pskKey.length() % 32 != 0 || pskKey.length() > 128) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key length = " + pskKey.length() + ". Key must be HexDec format: 32, 64, 128 characters!");
                }
                break;
            case RPK:
                @NotNull RPKBootstrapClientCredential rpkServerCredentials = (RPKBootstrapClientCredential) serverCredentials;
                if (StringUtils.isEmpty(rpkServerCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client RPK public key or id must be specified!");
                }
                try {
                    @NotNull String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getClientPublicKeyOrId());
                    rpkServerCredentials.setClientPublicKeyOrId(pubkRpkSever);
                    SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedClientPublicKeyOrId());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client RPK public key or id must be in standard [RFC7250 ] and then encoded to Base64 format!");
                }

                if (StringUtils.isEmpty(rpkServerCredentials.getClientSecretKey())) {
                    throw new DeviceCredentialsValidationException(server + " client RPK secret key must be specified!");
                }

                try {
                    @NotNull String prikRpkSever = EncryptionUtil.prikTrimNewLines(rpkServerCredentials.getClientSecretKey());
                    rpkServerCredentials.setClientSecretKey(prikRpkSever);
                    SecurityUtil.privateKey.decode(rpkServerCredentials.getDecodedClientSecretKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client RPK secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!");
                }
                break;
            case X509:
                @NotNull X509BootstrapClientCredential x509ServerCredentials = (X509BootstrapClientCredential) serverCredentials;
                if (StringUtils.isBlank(x509ServerCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client X509 public key or id must be specified!");
                }

                try {
                    @NotNull String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getClientPublicKeyOrId());
                    x509ServerCredentials.setClientPublicKeyOrId(certServer);
                    SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedClientPublicKeyOrId());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client X509 public key or id must be in DER-encoded X509v3 format  and support only EC algorithm and then encoded to Base64 format!");
                }
                if (StringUtils.isBlank(x509ServerCredentials.getClientSecretKey())) {
                    throw new DeviceCredentialsValidationException(server + " client X509 secret key must be specified!");
                }

                try {
                    @NotNull String prikX509Sever = EncryptionUtil.prikTrimNewLines(x509ServerCredentials.getClientSecretKey());
                    x509ServerCredentials.setClientSecretKey(prikX509Sever);
                    SecurityUtil.privateKey.decode(x509ServerCredentials.getDecodedClientSecretKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client X509 secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!");
                }
                break;
        }
    }

    @Override
    public void deleteDeviceCredentials(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        log.trace("Executing deleteDeviceCredentials [{}]", deviceCredentials);
        deviceCredentialsDao.removeById(tenantId, deviceCredentials.getUuidId());
        publishEvictEvent(new DeviceCredentialsEvictEvent(deviceCredentials.getCredentialsId(), null));
    }

}
