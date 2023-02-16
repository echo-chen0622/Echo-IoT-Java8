package org.echoiot.server.transport.coap;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.CoapDeviceType;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.echoiot.server.common.data.TransportPayloadType;

@Data
@Builder
public class CoapTestConfigProperties {

    String deviceName;

    CoapDeviceType coapDeviceType;

    TransportPayloadType transportPayloadType;

    String telemetryTopicFilter;
    String attributesTopicFilter;

    String telemetryProtoSchema;
    String attributesProtoSchema;
    String rpcResponseProtoSchema;
    String rpcRequestProtoSchema;

    DeviceProfileProvisionType provisionType;
    String provisionKey;
    String provisionSecret;

}
