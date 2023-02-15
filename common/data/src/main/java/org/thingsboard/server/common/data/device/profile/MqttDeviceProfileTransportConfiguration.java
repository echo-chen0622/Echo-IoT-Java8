package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
public class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @NoXss
    private String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    @NoXss
    private String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
    private boolean sendAckOnValidationException;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MQTT;
    }

    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        if (transportPayloadTypeConfiguration != null) {
            return transportPayloadTypeConfiguration;
        } else {
            return new JsonTransportPayloadConfiguration();
        }
    }


}
