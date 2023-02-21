package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.CoapDeviceType;
import org.jetbrains.annotations.NotNull;

@Data
public class DefaultCoapDeviceTypeConfiguration implements CoapDeviceTypeConfiguration {

    private static final long serialVersionUID = -4287100699186773773L;

    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;

    @NotNull
    @Override
    public CoapDeviceType getCoapDeviceType() {
        return CoapDeviceType.DEFAULT;
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
