package org.echoiot.server.dao.service.validator;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.RPKLwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.X509LwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.msg.EncryptionUtil;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceDao;
import org.echoiot.server.dao.device.DeviceProfileDao;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.DeviceCredentialsValidationException;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.tenant.TenantService;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DeviceProfileDataValidator extends AbstractHasOtaPackageValidator<DeviceProfile> {

    private static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    private static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    private static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    private static final String EXCEPTION_PREFIX = "[Transport Configuration]";

    @Resource
    private DeviceProfileDao deviceProfileDao;
    @Resource
    @Lazy
    private DeviceProfileService deviceProfileService;
    @Resource
    private DeviceDao deviceDao;
    @Resource
    private TenantService tenantService;
    @Lazy
    @Resource
    private QueueService queueService;
    @Resource
    private RuleChainService ruleChainService;
    @Resource
    private DashboardService dashboardService;

    @Override
    protected void validateDataImpl(TenantId tenantId, DeviceProfile deviceProfile) {
        if (StringUtils.isEmpty(deviceProfile.getName())) {
            throw new DataValidationException("Device profile name should be specified!");
        }
        if (deviceProfile.getType() == null) {
            throw new DataValidationException("Device profile type should be specified!");
        }
        if (deviceProfile.getTransportType() == null) {
            throw new DataValidationException("Device profile transport type should be specified!");
        }
        if (deviceProfile.getTenantId() == null) {
            throw new DataValidationException("Device profile should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(deviceProfile.getTenantId())) {
                throw new DataValidationException("Device profile is referencing to non-existent tenant!");
            }
        }
        if (deviceProfile.isDefault()) {
            DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
            if (defaultDeviceProfile != null && !defaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                throw new DataValidationException("Another default device profile is present in scope of current tenant!");
            }
        }
        if (StringUtils.isNotEmpty(deviceProfile.getDefaultQueueName())) {
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, deviceProfile.getDefaultQueueName());
            if (queue == null) {
                throw new DataValidationException("Device profile is referencing to non-existent queue!");
            }
        }
        if (deviceProfile.getProvisionType() == null) {
            deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        }
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        transportConfiguration.validate();
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            if (mqttTransportConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration) {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                        (ProtoTransportPayloadConfiguration) mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
                validateProtoSchemas(protoTransportPayloadConfiguration);
                validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
            }
        } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
            if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                if (transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration) {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                    validateProtoSchemas(protoTransportPayloadConfiguration);
                    validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                    validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
                }
            }
        } else if (transportConfiguration instanceof Lwm2mDeviceProfileTransportConfiguration) {
            List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations = ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).getBootstrap();
            if (lwM2MBootstrapServersConfigurations != null) {
                validateLwm2mServersConfigOfBootstrapForClient(lwM2MBootstrapServersConfigurations,
                        ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).isBootstrapServerUpdateEnable());
                for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
                    validateLwm2mServersCredentialOfBootstrapForClient(bootstrapServerCredential);
                }
            }
        }

        List<DeviceProfileAlarm> profileAlarms = deviceProfile.getProfileData().getAlarms();

        if (!CollectionUtils.isEmpty(profileAlarms)) {
            Set<String> alarmTypes = new HashSet<>();
            for (DeviceProfileAlarm alarm : profileAlarms) {
                String alarmType = alarm.getAlarmType();
                if (StringUtils.isEmpty(alarmType)) {
                    throw new DataValidationException("Alarm rule type should be specified!");
                }
                if (!alarmTypes.add(alarmType)) {
                    throw new DataValidationException(String.format("Can't create device profile with the same alarm rule types: \"%s\"!", alarmType));
                }
            }
        }

        if (deviceProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, deviceProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
            if (!ruleChain.getTenantId().equals(deviceProfile.getTenantId())) {
                throw new DataValidationException("Can't assign rule chain from different tenant!");
            }
        }

        if (deviceProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, deviceProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
            if (!dashboard.getTenantId().equals(deviceProfile.getTenantId())) {
                throw new DataValidationException("Can't assign dashboard from different tenant!");
            }
        }

        validateOtaPackage(tenantId, deviceProfile, deviceProfile.getId());
    }

    @Override
    protected DeviceProfile validateUpdate(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfile old = deviceProfileDao.findById(deviceProfile.getTenantId(), deviceProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing device profile!");
        }
        boolean profileTypeChanged = !old.getType().equals(deviceProfile.getType());
        boolean transportTypeChanged = !old.getTransportType().equals(deviceProfile.getTransportType());
        if (profileTypeChanged || transportTypeChanged) {
            Long profileDeviceCount = deviceDao.countDevicesByDeviceProfileId(deviceProfile.getTenantId(), deviceProfile.getId().getId());
            if (profileDeviceCount > 0) {
                @Nullable String message = null;
                if (profileTypeChanged) {
                    message = "Can't change device profile type because devices referenced it!";
                } else if (transportTypeChanged) {
                    message = "Can't change device profile transport type because devices referenced it!";
                }
                throw new DataValidationException(message);
            }
        }
        return old;
    }

    private void validateProtoSchemas(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        try {
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceAttributesProtoSchema(), ATTRIBUTES_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema(), TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema(), RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcResponseProtoSchema(), RPC_RESPONSE_PROTO_SCHEMA, EXCEPTION_PREFIX);
        } catch (Exception exception) {
            throw new DataValidationException(exception.getMessage());
        }
    }


    private void validateTelemetryDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        String deviceTelemetryProtoSchema = protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema();
        Descriptors.Descriptor telemetryDynamicMessageDescriptor = protoTransportPayloadTypeConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema);
        if (telemetryDynamicMessageDescriptor == null) {
            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get telemetryDynamicMessageDescriptor!");
        } else {
            List<Descriptors.FieldDescriptor> fields = telemetryDynamicMessageDescriptor.getFields();
            if (CollectionUtils.isEmpty(fields)) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + telemetryDynamicMessageDescriptor.getName() + " fields is empty!");
            } else if (fields.size() == 2) {
                Descriptors.FieldDescriptor tsFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("ts");
                Descriptors.FieldDescriptor valuesFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("values");
                if (tsFieldDescriptor != null && valuesFieldDescriptor != null) {
                    if (!Descriptors.FieldDescriptor.Type.MESSAGE.equals(valuesFieldDescriptor.getType())) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'values' has invalid data type. Only message type is supported!");
                    }
                    if (!Descriptors.FieldDescriptor.Type.INT64.equals(tsFieldDescriptor.getType())) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid data type. Only int64 type is supported!");
                    }
                    if (!tsFieldDescriptor.hasOptionalKeyword()) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid label. Field 'ts' should have optional keyword!");
                    }
                }
            }
        }
    }

    private void validateRpcRequestDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        DynamicMessage.Builder rpcRequestDynamicMessageBuilder = protoTransportPayloadTypeConfiguration.getRpcRequestDynamicMessageBuilder(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema());
        Descriptors.Descriptor rpcRequestDynamicMessageDescriptor = rpcRequestDynamicMessageBuilder.getDescriptorForType();
        if (rpcRequestDynamicMessageDescriptor == null) {
            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get rpcRequestDynamicMessageDescriptor!");
        } else {
            if (CollectionUtils.isEmpty(rpcRequestDynamicMessageDescriptor.getFields()) || rpcRequestDynamicMessageDescriptor.getFields().size() != 3) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + rpcRequestDynamicMessageDescriptor.getName() + " message should always contains 3 fields: method, requestId and params!");
            }
            Descriptors.FieldDescriptor methodFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("method");
            if (methodFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: method!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.STRING.equals(methodFieldDescriptor.getType())) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid data type. Only string type is supported!");
                }
                if (methodFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor requestIdFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("requestId");
            if (requestIdFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: requestId!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.INT32.equals(requestIdFieldDescriptor.getType())) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid data type. Only int32 type is supported!");
                }
                if (requestIdFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor paramsFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("params");
            if (paramsFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: params!");
            } else {
                if (paramsFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'params' has invalid label!");
                }
            }
        }
    }

    private void validateLwm2mServersConfigOfBootstrapForClient(List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations, boolean isBootstrapServerUpdateEnable) {
        Set<String> uris = new HashSet<>();
        Set<Integer> shortServerIds = new HashSet<>();
        for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
            AbstractLwM2MBootstrapServerCredential serverConfig = (AbstractLwM2MBootstrapServerCredential) bootstrapServerCredential;
            if (!isBootstrapServerUpdateEnable && serverConfig.isBootstrapServerIs()) {
                throw new DeviceCredentialsValidationException("Bootstrap config must not include \"Bootstrap Server\". \"Include Bootstrap Server updates\" is " + isBootstrapServerUpdateEnable + ".");
            }
            String server = serverConfig.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server" + " shortServerId: " + serverConfig.getShortServerId() + ":";
            if (serverConfig.getShortServerId() < 1 || serverConfig.getShortServerId() > 65534) {
                throw new DeviceCredentialsValidationException(server + " ShortServerId must not be less than 1 and more than 65534!");
            }
            if (!shortServerIds.add(serverConfig.getShortServerId())) {
                throw new DeviceCredentialsValidationException(server + " \"Short server Id\" value = " + serverConfig.getShortServerId() + ". This value must be a unique value for all servers!");
            }
            String uri = serverConfig.getHost() + ":" + serverConfig.getPort();
            if (!uris.add(uri)) {
                throw new DeviceCredentialsValidationException(server + " \"Host + port\" value = " + uri + ". This value must be a unique value for all servers!");
            }
            Integer port;
            if (LwM2MSecurityMode.NO_SEC.equals(serverConfig.getSecurityMode())) {
                port = serverConfig.isBootstrapServerIs() ? 5687 : 5685;
            } else {
                port = serverConfig.isBootstrapServerIs() ? 5688 : 5686;
            }
            if (serverConfig.getPort() == null || serverConfig.getPort().intValue() != port) {
                throw new DeviceCredentialsValidationException(server + " \"Port\" value = " + serverConfig.getPort() + ". This value for security " + serverConfig.getSecurityMode().name() + " must be " + port + "!");
            }
        }
    }

    private void validateLwm2mServersCredentialOfBootstrapForClient(LwM2MBootstrapServerCredential bootstrapServerConfig) {
        String server;
        switch (bootstrapServerConfig.getSecurityMode()) {
            case NO_SEC:
            case PSK:
                break;
            case RPK:
                RPKLwM2MBootstrapServerCredential rpkServerCredentials = (RPKLwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = rpkServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(rpkServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be specified!");
                }
                try {
                    String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getServerPublicKey());
                    rpkServerCredentials.setServerPublicKey(pubkRpkSever);
                    SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be in standard [RFC7250] and then encoded to Base64 format!");
                }
                break;
            case X509:
                X509LwM2MBootstrapServerCredential x509ServerCredentials = (X509LwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = x509ServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(x509ServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be specified!");
                }

                try {
                    String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getServerPublicKey());
                    x509ServerCredentials.setServerPublicKey(certServer);
                    SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                }
                break;
        }
    }
}
