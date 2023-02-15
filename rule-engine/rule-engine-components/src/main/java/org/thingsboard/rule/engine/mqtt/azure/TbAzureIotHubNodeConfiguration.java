package org.thingsboard.rule.engine.mqtt.azure;

import lombok.Data;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;

@Data
public class TbAzureIotHubNodeConfiguration extends TbMqttNodeConfiguration {

    @Override
    public TbAzureIotHubNodeConfiguration defaultConfiguration() {
        TbAzureIotHubNodeConfiguration configuration = new TbAzureIotHubNodeConfiguration();
        configuration.setTopicPattern("devices/<device_id>/messages/events/");
        configuration.setHost("<iot-hub-name>.azure-devices.net");
        configuration.setPort(8883);
        configuration.setConnectTimeoutSec(10);
        configuration.setCleanSession(true);
        configuration.setSsl(true);
        configuration.setCredentials(new AzureIotHubSasCredentials());
        return configuration;
    }

}
