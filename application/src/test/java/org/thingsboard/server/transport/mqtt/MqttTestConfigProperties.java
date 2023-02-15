package org.thingsboard.server.transport.mqtt;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;

@Data
@Builder
public class MqttTestConfigProperties {

    String deviceName;
    String gatewayName;

    TransportPayloadType transportPayloadType;

    String telemetryTopicFilter;
    String attributesTopicFilter;

    String telemetryProtoSchema;
    String attributesProtoSchema;
    String rpcResponseProtoSchema;
    String rpcRequestProtoSchema;

    boolean enableCompatibilityWithJsonPayloadFormat;
    boolean useJsonPayloadFormatForDefaultDownlinkTopics;
    boolean sendAckOnValidationException;

    DeviceProfileProvisionType provisionType;
    String provisionKey;
    String provisionSecret;

}
