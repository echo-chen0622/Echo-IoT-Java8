package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.device.data.PowerSavingConfiguration;
import org.echoiot.server.common.data.DeviceTransportType;
import org.jetbrains.annotations.NotNull;

@Data
public class CoapDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private CoapDeviceTypeConfiguration coapDeviceTypeConfiguration;
    private PowerSavingConfiguration clientSettings;

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.COAP;
    }

    @NotNull
    public CoapDeviceTypeConfiguration getCoapDeviceTypeConfiguration() {
        if (coapDeviceTypeConfiguration != null) {
            return coapDeviceTypeConfiguration;
        } else {
            return new DefaultCoapDeviceTypeConfiguration();
        }
    }
}
