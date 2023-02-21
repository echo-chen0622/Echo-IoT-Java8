package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;

@Data
public class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @NotNull
    @NoXss
    private String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    @NotNull
    @NoXss
    private String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
    private boolean sendAckOnValidationException;

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MQTT;
    }

    @NotNull
    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        if (transportPayloadTypeConfiguration != null) {
            return transportPayloadTypeConfiguration;
        } else {
            return new JsonTransportPayloadConfiguration();
        }
    }


}
