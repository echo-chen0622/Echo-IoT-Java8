package org.echoiot.server.service.device;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.echoiot.server.common.data.device.data.PowerMode;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.exception.DeviceCredentialsValidationException;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.device.TbDeviceService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.sync.ie.importing.csv.AbstractBulkImportService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceBulkImportService extends AbstractBulkImportService<Device> {
    protected final DeviceService deviceService;
    protected final TbDeviceService tbDeviceService;
    protected final DeviceCredentialsService deviceCredentialsService;
    protected final DeviceProfileService deviceProfileService;

    private final Lock findOrCreateDeviceProfileLock = new ReentrantLock();

    @Override
    protected void setEntityFields(Device entity, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case IS_GATEWAY:
                    additionalInfo.set("gateway", BooleanNode.valueOf(Boolean.parseBoolean(value)));
                    break;
            }
            entity.setAdditionalInfo(additionalInfo);
        });
    }

    @Override
    @SneakyThrows
    protected Device saveEntity(SecurityUser user, Device entity, Map<BulkImportColumnType, String> fields) {
        DeviceCredentials deviceCredentials;
        try {
            deviceCredentials = createDeviceCredentials(entity.getTenantId(), entity.getId(), fields);
            deviceCredentialsService.formatCredentials(deviceCredentials);
        } catch (Exception e) {
            throw new DeviceCredentialsValidationException("Invalid device credentials: " + e.getMessage());
        }

        DeviceProfile deviceProfile;
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
            deviceProfile = setUpLwM2mDeviceProfile(entity.getTenantId(), entity);
        } else if (StringUtils.isNotEmpty(entity.getType())) {
            deviceProfile = deviceProfileService.findOrCreateDeviceProfile(entity.getTenantId(), entity.getType());
        } else {
            deviceProfile = deviceProfileService.findDefaultDeviceProfile(entity.getTenantId());
        }
        entity.setDeviceProfileId(deviceProfile.getId());

        return tbDeviceService.saveDeviceWithCredentials(entity, deviceCredentials, user);
    }

    @Override
    protected Device findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(deviceService.findDeviceByTenantIdAndName(tenantId, name))
                .orElseGet(Device::new);
    }

    @Override
    protected void setOwners(Device entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    @SneakyThrows
    private DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceId deviceId, Map<BulkImportColumnType, String> fields) {
        DeviceCredentials credentials = new DeviceCredentials();
        if (fields.containsKey(BulkImportColumnType.LWM2M_CLIENT_ENDPOINT)) {
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            setUpLwm2mCredentials(fields, credentials);
        } else if (fields.containsKey(BulkImportColumnType.X509)) {
            credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
            setUpX509CertificateCredentials(fields, credentials);
        } else if (CollectionUtils.containsAny(fields.keySet(), EnumSet.of(BulkImportColumnType.MQTT_CLIENT_ID, BulkImportColumnType.MQTT_USER_NAME, BulkImportColumnType.MQTT_PASSWORD))) {
            credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
            setUpBasicMqttCredentials(fields, credentials);
        } else if (deviceId != null && !fields.containsKey(BulkImportColumnType.ACCESS_TOKEN)) {
            credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        } else  {
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            setUpAccessTokenCredentials(fields, credentials);
        }
        return credentials;
    }

    private void setUpAccessTokenCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        credentials.setCredentialsId(Optional.ofNullable(fields.get(BulkImportColumnType.ACCESS_TOKEN))
                .orElseGet(() -> StringUtils.randomAlphanumeric(20)));
    }

    private void setUpBasicMqttCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        BasicMqttCredentials basicMqttCredentials = new BasicMqttCredentials();
        basicMqttCredentials.setClientId(fields.get(BulkImportColumnType.MQTT_CLIENT_ID));
        basicMqttCredentials.setUserName(fields.get(BulkImportColumnType.MQTT_USER_NAME));
        basicMqttCredentials.setPassword(fields.get(BulkImportColumnType.MQTT_PASSWORD));
        credentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));
    }

    private void setUpX509CertificateCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        credentials.setCredentialsValue(fields.get(BulkImportColumnType.X509));
    }

    private void setUpLwm2mCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) throws com.fasterxml.jackson.core.JsonProcessingException {
        ObjectNode lwm2mCredentials = JacksonUtil.newObjectNode();

        Set.of(BulkImportColumnType.LWM2M_CLIENT_SECURITY_CONFIG_MODE, BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE,
                        BulkImportColumnType.LWM2M_SERVER_SECURITY_MODE).stream()
                .map(fields::get)
                .filter(Objects::nonNull)
                .forEach(securityMode -> {
                    try {
                        LwM2MSecurityMode.valueOf(securityMode.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new DeviceCredentialsValidationException("Unknown LwM2M security mode: " + securityMode + ", (the mode should be: NO_SEC, PSK, RPK, X509)!");
                    }
                });

        ObjectNode client = JacksonUtil.newObjectNode();
        setValues(client, fields, Set.of(BulkImportColumnType.LWM2M_CLIENT_SECURITY_CONFIG_MODE,
                BulkImportColumnType.LWM2M_CLIENT_ENDPOINT, BulkImportColumnType.LWM2M_CLIENT_IDENTITY,
                BulkImportColumnType.LWM2M_CLIENT_KEY, BulkImportColumnType.LWM2M_CLIENT_CERT));
        LwM2MClientCredential lwM2MClientCredential = JacksonUtil.treeToValue(client, LwM2MClientCredential.class);
        // so that only fields needed for specific type of lwM2MClientCredentials were saved in json
        lwm2mCredentials.set("client", JacksonUtil.valueToTree(lwM2MClientCredential));

        ObjectNode bootstrapServer = JacksonUtil.newObjectNode();
        setValues(bootstrapServer, fields, Set.of(BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE,
                BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID, BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECRET_KEY));

        ObjectNode lwm2mServer = JacksonUtil.newObjectNode();
        setValues(lwm2mServer, fields, Set.of(BulkImportColumnType.LWM2M_SERVER_SECURITY_MODE,
                BulkImportColumnType.LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID, BulkImportColumnType.LWM2M_SERVER_CLIENT_SECRET_KEY));

        ObjectNode bootstrap = JacksonUtil.newObjectNode();
        bootstrap.set("bootstrapServer", bootstrapServer);
        bootstrap.set("lwm2mServer", lwm2mServer);
        lwm2mCredentials.set("bootstrap", bootstrap);

        credentials.setCredentialsValue(lwm2mCredentials.toString());
    }

    private DeviceProfile setUpLwM2mDeviceProfile(TenantId tenantId, Device device) {
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByName(tenantId, device.getType());
        if (deviceProfile != null) {
            if (deviceProfile.getTransportType() != DeviceTransportType.LWM2M) {
                deviceProfile.setTransportType(DeviceTransportType.LWM2M);
                deviceProfile.getProfileData().setTransportConfiguration(new Lwm2mDeviceProfileTransportConfiguration());
                deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
            }
        } else {
            findOrCreateDeviceProfileLock.lock();
            try {
                deviceProfile = deviceProfileService.findDeviceProfileByName(tenantId, device.getType());
                if (deviceProfile == null) {
                    deviceProfile = new DeviceProfile();
                    deviceProfile.setTenantId(tenantId);
                    deviceProfile.setType(DeviceProfileType.DEFAULT);
                    deviceProfile.setName(device.getType());
                    deviceProfile.setTransportType(DeviceTransportType.LWM2M);
                    deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);

                    Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
                    transportConfiguration.setBootstrap(Collections.emptyList());
                    transportConfiguration.setClientLwM2mSettings(new OtherConfiguration(1, 1, 1, PowerMode.DRX, null, null, null, null, null));
                    transportConfiguration.setObserveAttr(new TelemetryMappingConfiguration(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap()));

                    DeviceProfileData deviceProfileData = new DeviceProfileData();
                    DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
                    DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
                    deviceProfileData.setConfiguration(configuration);
                    deviceProfileData.setTransportConfiguration(transportConfiguration);
                    deviceProfileData.setProvisionConfiguration(provisionConfiguration);
                    deviceProfile.setProfileData(deviceProfileData);

                    deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
                }
            } finally {
                findOrCreateDeviceProfileLock.unlock();
            }
        }
        return deviceProfile;
    }

    private void setValues(ObjectNode objectNode, Map<BulkImportColumnType, String> data, Collection<BulkImportColumnType> columns) {
        for (BulkImportColumnType column : columns) {
            String value = StringUtils.defaultString(data.get(column), column.getDefaultValue());
            if (value != null && column.getKey() != null) {
                objectNode.set(column.getKey(), new TextNode(value));
            }
        }
    }

    @Override
    protected EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
