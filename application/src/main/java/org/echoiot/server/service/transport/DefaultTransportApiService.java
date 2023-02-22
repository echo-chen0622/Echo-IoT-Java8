package org.echoiot.server.service.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.cache.ota.OtaPackageDataCache;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.echoiot.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.data.PowerMode;
import org.echoiot.server.common.data.device.data.PowerSavingConfiguration;
import org.echoiot.server.common.data.device.profile.ProvisionDeviceProfileCredentials;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.ota.OtaPackageUtil;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProvisionService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.device.provision.ProvisionFailedException;
import org.echoiot.server.dao.device.provision.ProvisionRequest;
import org.echoiot.server.dao.device.provision.ProvisionResponse;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.apiusage.TbApiUsageStateService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.echoiot.server.service.resource.TbResourceService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by Echo on 05.10.18.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTransportApiService implements TransportApiService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final TbDeviceProfileCache deviceProfileCache;
    private final TbTenantProfileCache tenantProfileCache;
    private final TbApiUsageStateService apiUsageStateService;
    private final DeviceService deviceService;
    private final RelationService relationService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final TbClusterService tbClusterService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final DeviceProvisionService deviceProvisionService;
    private final TbResourceService resourceService;
    private final OtaPackageService otaPackageService;
    private final OtaPackageDataCache otaPackageDataCache;
    private final QueueService queueService;

    private final ConcurrentMap<String, ReentrantLock> deviceCreationLocks = new ConcurrentHashMap<>();

    private static boolean checkIsMqttCredentials(@Nullable DeviceCredentials credentials) {
        return credentials != null && DeviceCredentialsType.MQTT_BASIC.equals(credentials.getCredentialsType());
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<TransportApiResponseMsg>> handle(TbProtoQueueMsg<TransportApiRequestMsg> tbProtoQueueMsg) {
        TransportApiRequestMsg transportApiRequestMsg = tbProtoQueueMsg.getValue();
        @Nullable ListenableFuture<TransportApiResponseMsg> result = null;

        if (transportApiRequestMsg.hasValidateTokenRequestMsg()) {
            ValidateDeviceTokenRequestMsg msg = transportApiRequestMsg.getValidateTokenRequestMsg();
            result = validateCredentials(msg.getToken(), DeviceCredentialsType.ACCESS_TOKEN);
        } else if (transportApiRequestMsg.hasValidateBasicMqttCredRequestMsg()) {
            TransportProtos.ValidateBasicMqttCredRequestMsg msg = transportApiRequestMsg.getValidateBasicMqttCredRequestMsg();
            result = validateCredentials(msg);
        } else if (transportApiRequestMsg.hasValidateX509CertRequestMsg()) {
            ValidateDeviceX509CertRequestMsg msg = transportApiRequestMsg.getValidateX509CertRequestMsg();
            result = validateCredentials(msg.getHash(), DeviceCredentialsType.X509_CERTIFICATE);
        } else if (transportApiRequestMsg.hasGetOrCreateDeviceRequestMsg()) {
            result = handle(transportApiRequestMsg.getGetOrCreateDeviceRequestMsg());
        } else if (transportApiRequestMsg.hasEntityProfileRequestMsg()) {
            result = handle(transportApiRequestMsg.getEntityProfileRequestMsg());
        } else if (transportApiRequestMsg.hasLwM2MRequestMsg()) {
            result = handle(transportApiRequestMsg.getLwM2MRequestMsg());
        } else if (transportApiRequestMsg.hasValidateDeviceLwM2MCredentialsRequestMsg()) {
            ValidateDeviceLwM2MCredentialsRequestMsg msg = transportApiRequestMsg.getValidateDeviceLwM2MCredentialsRequestMsg();
            result = validateCredentials(msg.getCredentialsId(), DeviceCredentialsType.LWM2M_CREDENTIALS);
        } else if (transportApiRequestMsg.hasProvisionDeviceRequestMsg()) {
            result = handle(transportApiRequestMsg.getProvisionDeviceRequestMsg());
        } else if (transportApiRequestMsg.hasResourceRequestMsg()) {
            result = handle(transportApiRequestMsg.getResourceRequestMsg());
        } else if (transportApiRequestMsg.hasSnmpDevicesRequestMsg()) {
            result = handle(transportApiRequestMsg.getSnmpDevicesRequestMsg());
        } else if (transportApiRequestMsg.hasDeviceRequestMsg()) {
            result = handle(transportApiRequestMsg.getDeviceRequestMsg());
        } else if (transportApiRequestMsg.hasDeviceCredentialsRequestMsg()) {
            result = handle(transportApiRequestMsg.getDeviceCredentialsRequestMsg());
        } else if (transportApiRequestMsg.hasOtaPackageRequestMsg()) {
            result = handle(transportApiRequestMsg.getOtaPackageRequestMsg());
        } else if (transportApiRequestMsg.hasGetAllQueueRoutingInfoRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getGetAllQueueRoutingInfoRequestMsg()), value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        }

        return Futures.transform(Optional.ofNullable(result).orElseGet(this::getEmptyTransportApiResponseFuture),
                value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<TransportApiResponseMsg> validateCredentials(String credentialsId, DeviceCredentialsType credentialsType) {
        //TODO: Make async and enable caching
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        if (credentials != null && credentials.getCredentialsType() == credentialsType) {
            return getDeviceInfo(credentials);
        } else {
            return getEmptyTransportApiResponseFuture();
        }
    }

    private ListenableFuture<TransportApiResponseMsg> validateCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg mqtt) {
        @Nullable DeviceCredentials credentials;
        if (StringUtils.isEmpty(mqtt.getUserName())) {
            credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash(mqtt.getClientId()));
            if (credentials != null) {
                return getDeviceInfo(credentials);
            } else {
                return getEmptyTransportApiResponseFuture();
            }
        } else {
            credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(
                    EncryptionUtil.getSha3Hash("|", mqtt.getClientId(), mqtt.getUserName()));
            if (checkIsMqttCredentials(credentials)) {
                var validationResult = validateMqttCredentials(mqtt, credentials);
                if (BasicCredentialsValidationResult.VALID.equals(validationResult)) {
                    return getDeviceInfo(credentials);
                } else if (BasicCredentialsValidationResult.PASSWORD_MISMATCH.equals(validationResult)) {
                    return getEmptyTransportApiResponseFuture();
                } else {
                    return validateUserNameCredentials(mqtt);
                }
            } else {
                return validateUserNameCredentials(mqtt);
            }
        }
    }

    private ListenableFuture<TransportApiResponseMsg> validateUserNameCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg mqtt) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(mqtt.getUserName());
        if (credentials != null) {
            switch (credentials.getCredentialsType()) {
                case ACCESS_TOKEN:
                    return getDeviceInfo(credentials);
                case MQTT_BASIC:
                    if (BasicCredentialsValidationResult.VALID.equals(validateMqttCredentials(mqtt, credentials))) {
                        return getDeviceInfo(credentials);
                    } else {
                        return getEmptyTransportApiResponseFuture();
                    }
            }
        }
        return getEmptyTransportApiResponseFuture();
    }

    @Nullable
    private DeviceCredentials checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, String credId) {
        return checkMqttCredentials(clientCred, deviceCredentialsService.findDeviceCredentialsByCredentialsId(credId));
    }

    private DeviceCredentials checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, @Nullable DeviceCredentials deviceCredentials) {
        if (deviceCredentials != null && deviceCredentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
            if (BasicCredentialsValidationResult.VALID.equals(validateMqttCredentials(clientCred, deviceCredentials))) {
                return deviceCredentials;
            }
        }
        return null;
    }

    private BasicCredentialsValidationResult validateMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, DeviceCredentials deviceCredentials) {
        @Nullable BasicMqttCredentials dbCred = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
        if (!StringUtils.isEmpty(dbCred.getClientId()) && !dbCred.getClientId().equals(clientCred.getClientId())) {
            return BasicCredentialsValidationResult.HASH_MISMATCH;
        }
        if (!StringUtils.isEmpty(dbCred.getUserName()) && !dbCred.getUserName().equals(clientCred.getUserName())) {
            return BasicCredentialsValidationResult.HASH_MISMATCH;
        }
        if (!StringUtils.isEmpty(dbCred.getPassword())) {
            if (StringUtils.isEmpty(clientCred.getPassword())) {
                return BasicCredentialsValidationResult.PASSWORD_MISMATCH;
            } else {
                return dbCred.getPassword().equals(clientCred.getPassword()) ? BasicCredentialsValidationResult.VALID : BasicCredentialsValidationResult.PASSWORD_MISMATCH;
            }
        }
        return BasicCredentialsValidationResult.VALID;
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetOrCreateDeviceFromGatewayRequestMsg requestMsg) {
        DeviceId gatewayId = new DeviceId(new UUID(requestMsg.getGatewayIdMSB(), requestMsg.getGatewayIdLSB()));
        ListenableFuture<Device> gatewayFuture = deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, gatewayId);
        return Futures.transform(gatewayFuture, gateway -> {
            Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(requestMsg.getDeviceName(), id -> new ReentrantLock());
            deviceCreationLock.lock();
            try {
                Device device = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), requestMsg.getDeviceName());
                if (device == null) {
                    TenantId tenantId = gateway.getTenantId();
                    device = new Device();
                    device.setTenantId(tenantId);
                    device.setName(requestMsg.getDeviceName());
                    device.setType(requestMsg.getDeviceType());
                    device.setCustomerId(gateway.getCustomerId());
                    DeviceProfile deviceProfile = deviceProfileCache.findOrCreateDeviceProfile(gateway.getTenantId(), requestMsg.getDeviceType());
                    device.setDeviceProfileId(deviceProfile.getId());
                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                    additionalInfo.put(DataConstants.LAST_CONNECTED_GATEWAY, gatewayId.toString());
                    device.setAdditionalInfo(additionalInfo);
                    Device savedDevice = deviceService.saveDevice(device);
                    tbClusterService.onDeviceUpdated(savedDevice, null);
                    device = savedDevice;

                    relationService.saveRelation(TenantId.SYS_TENANT_ID, new EntityRelation(gateway.getId(), device.getId(), "Created"));

                    TbMsgMetaData metaData = new TbMsgMetaData();
                    CustomerId customerId = gateway.getCustomerId();
                    if (customerId != null && !customerId.isNullUid()) {
                        metaData.putValue("customerId", customerId.toString());
                    }
                    metaData.putValue("gatewayId", gatewayId.toString());

                    DeviceId deviceId = device.getId();
                    ObjectNode entityNode = mapper.valueToTree(device);
                    TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, customerId, metaData, TbMsgDataType.JSON, mapper.writeValueAsString(entityNode));
                    tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, null);
                } else {
                    JsonNode deviceAdditionalInfo = device.getAdditionalInfo();
                    if (deviceAdditionalInfo == null) {
                        deviceAdditionalInfo = JacksonUtil.newObjectNode();
                    }
                    if (deviceAdditionalInfo.isObject() &&
                            (!deviceAdditionalInfo.has(DataConstants.LAST_CONNECTED_GATEWAY)
                                    || !gatewayId.toString().equals(deviceAdditionalInfo.get(DataConstants.LAST_CONNECTED_GATEWAY).asText()))) {
                        ObjectNode newDeviceAdditionalInfo = (ObjectNode) deviceAdditionalInfo;
                        newDeviceAdditionalInfo.put(DataConstants.LAST_CONNECTED_GATEWAY, gatewayId.toString());
                        Device savedDevice = deviceService.saveDevice(device);
                        tbClusterService.onDeviceUpdated(savedDevice, device);
                    }
                }
                GetOrCreateDeviceFromGatewayResponseMsg.Builder builder = GetOrCreateDeviceFromGatewayResponseMsg.newBuilder()
                        .setDeviceInfo(getDeviceInfoProto(device));
                DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile != null) {
                    builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
                } else {
                    log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
                }
                return TransportApiResponseMsg.newBuilder()
                        .setGetOrCreateDeviceResponseMsg(builder.build())
                        .build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by gateway id and name: [{}]", gatewayId, requestMsg.getDeviceName(), e);
                throw new RuntimeException(e);
            } finally {
                deviceCreationLock.unlock();
            }
        }, dbCallbackExecutorService);
    }

    private ListenableFuture<TransportApiResponseMsg> handle(ProvisionDeviceRequestMsg requestMsg) {
        @Nullable ListenableFuture<ProvisionResponse> provisionResponseFuture = null;
        try {
            provisionResponseFuture = Futures.immediateFuture(deviceProvisionService.provisionDevice(
                    new ProvisionRequest(
                            requestMsg.getDeviceName(),
                            requestMsg.getCredentialsType() != null ? DeviceCredentialsType.valueOf(requestMsg.getCredentialsType().name()) : null,
                            new ProvisionDeviceCredentialsData(requestMsg.getCredentialsDataProto().getValidateDeviceTokenRequestMsg().getToken(),
                                                               requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getClientId(),
                                                               requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getUserName(),
                                                               requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getPassword(),
                                                               requestMsg.getCredentialsDataProto().getValidateDeviceX509CertRequestMsg().getHash()),
                            new ProvisionDeviceProfileCredentials(
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceKey(),
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceSecret()))));
        } catch (ProvisionFailedException e) {
            return Futures.immediateFuture(getTransportApiResponseMsg(
                    new DeviceCredentials(),
                    TransportProtos.ResponseStatus.valueOf(e.getMessage())));
        }
        return Futures.transform(provisionResponseFuture, provisionResponse -> getTransportApiResponseMsg(provisionResponse.getDeviceCredentials(), TransportProtos.ResponseStatus.SUCCESS),
                dbCallbackExecutorService);
    }

    private TransportApiResponseMsg getTransportApiResponseMsg(DeviceCredentials
                                                                       deviceCredentials, TransportProtos.ResponseStatus status) {
        if (!status.equals(TransportProtos.ResponseStatus.SUCCESS)) {
            return TransportApiResponseMsg.newBuilder().setProvisionDeviceResponseMsg(TransportProtos.ProvisionDeviceResponseMsg.newBuilder().setStatus(status).build()).build();
        }
        TransportProtos.ProvisionDeviceResponseMsg.Builder provisionResponse = TransportProtos.ProvisionDeviceResponseMsg.newBuilder()
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(deviceCredentials.getCredentialsType().name()))
                .setStatus(status);
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
            case X509_CERTIFICATE:
            case LWM2M_CREDENTIALS:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsValue());
                break;
        }

        return TransportApiResponseMsg.newBuilder()
                .setProvisionDeviceResponseMsg(provisionResponse.build())
                .build();
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetEntityProfileRequestMsg requestMsg) {
        EntityType entityType = EntityType.valueOf(requestMsg.getEntityType());
        UUID entityUuid = new UUID(requestMsg.getEntityIdMSB(), requestMsg.getEntityIdLSB());
        GetEntityProfileResponseMsg.Builder builder = GetEntityProfileResponseMsg.newBuilder();
        if (entityType.equals(EntityType.DEVICE_PROFILE)) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(entityUuid);
            DeviceProfile deviceProfile = deviceProfileCache.find(deviceProfileId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
        } else if (entityType.equals(EntityType.TENANT)) {
            TenantId tenantId = TenantId.fromUUID(entityUuid);
            @Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            ApiUsageState state = apiUsageStateService.getApiUsageState(tenantId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile)));
            builder.setApiState(ByteString.copyFrom(dataDecodingEncodingService.encode(state)));
        } else {
            throw new RuntimeException("Invalid entity profile request: " + entityType);
        }
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setEntityProfileResponseMsg(builder).build());
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetDeviceRequestMsg requestMsg) {
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);

        TransportApiResponseMsg responseMsg;
        if (device != null) {
            UUID deviceProfileId = device.getDeviceProfileId().getId();
            responseMsg = TransportApiResponseMsg.newBuilder()
                    .setDeviceResponseMsg(TransportProtos.GetDeviceResponseMsg.newBuilder()
                            .setDeviceProfileIdMSB(deviceProfileId.getMostSignificantBits())
                            .setDeviceProfileIdLSB(deviceProfileId.getLeastSignificantBits())
                            .setDeviceTransportConfiguration(ByteString.copyFrom(
                                    dataDecodingEncodingService.encode(device.getDeviceData().getTransportConfiguration())
                            )))
                    .build();
        } else {
            responseMsg = TransportApiResponseMsg.getDefaultInstance();
        }

        return Futures.immediateFuture(responseMsg);
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetDeviceCredentialsRequestMsg requestMsg) {
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, deviceId);

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .setDeviceCredentialsResponseMsg(TransportProtos.GetDeviceCredentialsResponseMsg.newBuilder()
                        .setDeviceCredentialsData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceCredentials))))
                .build());
    }


    private ListenableFuture<TransportApiResponseMsg> handle(GetResourceRequestMsg requestMsg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        ResourceType resourceType = ResourceType.valueOf(requestMsg.getResourceType());
        String resourceKey = requestMsg.getResourceKey();
        TransportProtos.GetResourceResponseMsg.Builder builder = TransportProtos.GetResourceResponseMsg.newBuilder();
        TbResource resource = resourceService.getResource(tenantId, resourceType, resourceKey);

        if (resource == null && !tenantId.equals(TenantId.SYS_TENANT_ID)) {
            resource = resourceService.getResource(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        }

        if (resource != null) {
            builder.setResource(ByteString.copyFrom(dataDecodingEncodingService.encode(resource)));
        }

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setResourceResponseMsg(builder).build());
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetSnmpDevicesRequestMsg requestMsg) {
        PageLink pageLink = new PageLink(requestMsg.getPageSize(), requestMsg.getPage());
        PageData<UUID> result = deviceService.findDevicesIdsByDeviceProfileTransportType(DeviceTransportType.SNMP, pageLink);

        GetSnmpDevicesResponseMsg responseMsg = GetSnmpDevicesResponseMsg.newBuilder()
                .addAllIds(result.getData().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()))
                .setHasNextPage(result.hasNext())
                .build();

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .setSnmpDevicesResponseMsg(responseMsg)
                .build());
    }

    private ListenableFuture<TransportApiResponseMsg> getDeviceInfo(DeviceCredentials credentials) {
        return Futures.transform(deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, credentials.getDeviceId()), device -> {
            if (device == null) {
                log.trace("[{}] Failed to lookup device by id", credentials.getDeviceId());
                return getEmptyTransportApiResponse();
            }
            try {
                ValidateDeviceCredentialsResponseMsg.Builder builder = ValidateDeviceCredentialsResponseMsg.newBuilder();
                builder.setDeviceInfo(getDeviceInfoProto(device));
                DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile != null) {
                    builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
                } else {
                    log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
                }
                if (!StringUtils.isEmpty(credentials.getCredentialsValue())) {
                    builder.setCredentialsBody(credentials.getCredentialsValue());
                }
                return TransportApiResponseMsg.newBuilder()
                        .setValidateCredResponseMsg(builder.build()).build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by id", credentials.getDeviceId(), e);
                return getEmptyTransportApiResponse();
            }
        }, MoreExecutors.directExecutor());
    }

    private DeviceInfoProto getDeviceInfoProto(Device device) throws JsonProcessingException {
        DeviceInfoProto.Builder builder = DeviceInfoProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(Optional.ofNullable(device.getCustomerId()).map(customerId -> customerId.getId().getMostSignificantBits()).orElse(0L))
                .setCustomerIdLSB(Optional.ofNullable(device.getCustomerId()).map(customerId -> customerId.getId().getLeastSignificantBits()).orElse(0L))
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setAdditionalInfo(mapper.writeValueAsString(device.getAdditionalInfo()));

        @Nullable PowerSavingConfiguration psmConfiguration = null;
        switch (device.getDeviceData().getTransportConfiguration().getType()) {
            case LWM2M:
                psmConfiguration = (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
            case COAP:
                psmConfiguration = (CoapDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
        }

        if (psmConfiguration != null) {
            PowerMode powerMode = psmConfiguration.getPowerMode();
            if (powerMode != null) {
                builder.setPowerMode(powerMode.name());
                if (powerMode.equals(PowerMode.PSM)) {
                    builder.setPsmActivityTimer(checkLong(psmConfiguration.getPsmActivityTimer()));
                } else if (powerMode.equals(PowerMode.E_DRX)) {
                    builder.setEdrxCycle(checkLong(psmConfiguration.getEdrxCycle()));
                    builder.setPagingTransmissionWindow(checkLong(psmConfiguration.getPagingTransmissionWindow()));
                }
            }
        }
        return builder.build();
    }

    private ListenableFuture<TransportApiResponseMsg> getEmptyTransportApiResponseFuture() {
        return Futures.immediateFuture(getEmptyTransportApiResponse());
    }

    private TransportApiResponseMsg getEmptyTransportApiResponse() {
        return TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(ValidateDeviceCredentialsResponseMsg.getDefaultInstance()).build();
    }

    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.LwM2MRequestMsg requestMsg) {
        if (requestMsg.hasRegistrationMsg()) {
            return handleRegistration(requestMsg.getRegistrationMsg());
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("Not supported!"));
        }
    }

    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.GetOtaPackageRequestMsg requestMsg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        OtaPackageType otaPackageType = OtaPackageType.valueOf(requestMsg.getType());
        Device device = deviceService.findDeviceById(tenantId, deviceId);

        if (device == null) {
            return getEmptyTransportApiResponseFuture();
        }

        @Nullable OtaPackageId otaPackageId = OtaPackageUtil.getOtaPackageId(device, otaPackageType);
        if (otaPackageId == null) {
            DeviceProfile deviceProfile = deviceProfileCache.find(device.getDeviceProfileId());
            otaPackageId = OtaPackageUtil.getOtaPackageId(deviceProfile, otaPackageType);
        }

        TransportProtos.GetOtaPackageResponseMsg.Builder builder = TransportProtos.GetOtaPackageResponseMsg.newBuilder();

        if (otaPackageId == null) {
            builder.setResponseStatus(TransportProtos.ResponseStatus.NOT_FOUND);
        } else {
            OtaPackageInfo otaPackageInfo = otaPackageService.findOtaPackageInfoById(tenantId, otaPackageId);

            if (otaPackageInfo == null) {
                builder.setResponseStatus(TransportProtos.ResponseStatus.NOT_FOUND);
            } else if (otaPackageInfo.hasUrl()) {
                builder.setResponseStatus(TransportProtos.ResponseStatus.FAILURE);
                log.trace("[{}] Can`t send OtaPackage with URL data!", otaPackageInfo.getId());
            } else {
                builder.setResponseStatus(TransportProtos.ResponseStatus.SUCCESS);
                builder.setOtaPackageIdMSB(otaPackageId.getId().getMostSignificantBits());
                builder.setOtaPackageIdLSB(otaPackageId.getId().getLeastSignificantBits());
                builder.setType(otaPackageInfo.getType().name());
                builder.setTitle(otaPackageInfo.getTitle());
                builder.setVersion(otaPackageInfo.getVersion());
                builder.setFileName(otaPackageInfo.getFileName());
                builder.setContentType(otaPackageInfo.getContentType());
                if (!otaPackageDataCache.has(otaPackageId.toString())) {
                    OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                    otaPackageDataCache.put(otaPackageId.toString(), otaPackage.getData().array());
                }
            }
        }

        return Futures.immediateFuture(
                TransportApiResponseMsg.newBuilder()
                        .setOtaPackageResponseMsg(builder.build())
                        .build());
    }

    private ListenableFuture<TransportApiResponseMsg> handleRegistration
            (TransportProtos.LwM2MRegistrationRequestMsg msg) {
        TenantId tenantId = TenantId.fromUUID(UUID.fromString(msg.getTenantId()));
        String deviceName = msg.getEndpoint();
        Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(deviceName, id -> new ReentrantLock());
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setName(deviceName);
                device.setType("LwM2M");
                device = deviceService.saveDevice(device);
                tbClusterService.onDeviceUpdated(device, null);
            }
            TransportProtos.LwM2MRegistrationResponseMsg registrationResponseMsg =
                    TransportProtos.LwM2MRegistrationResponseMsg.newBuilder()
                            .setDeviceInfo(getDeviceInfoProto(device)).build();
            TransportProtos.LwM2MResponseMsg responseMsg = TransportProtos.LwM2MResponseMsg.newBuilder().setRegistrationMsg(registrationResponseMsg).build();
            return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setLwM2MResponseMsg(responseMsg).build());
        } catch (JsonProcessingException e) {
            log.warn("[{}][{}] Failed to lookup device by gateway id and name", tenantId, deviceName, e);
            throw new RuntimeException(e);
        } finally {
            deviceCreationLock.unlock();
        }
    }

    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.GetAllQueueRoutingInfoRequestMsg requestMsg) {
        return queuesToTransportApiResponseMsg(queueService.findAllQueues());
    }

    private ListenableFuture<TransportApiResponseMsg> queuesToTransportApiResponseMsg(List<Queue> queues) {
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .addAllGetQueueRoutingInfoResponseMsgs(queues.stream()
                        .map(queue -> TransportProtos.GetQueueRoutingInfoResponseMsg.newBuilder()
                                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                                .setQueueName(queue.getName())
                                .setQueueTopic(queue.getTopic())
                                .setPartitions(queue.getPartitions())
                                .build()).collect(Collectors.toList())).build());
    }


    private Long checkLong(@Nullable Long l) {
        return l != null ? l : 0;
    }
}
