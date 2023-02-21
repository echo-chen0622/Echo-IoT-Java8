package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.CoapDeviceType;
import org.jetbrains.annotations.NotNull;

@Data
public class EfentoCoapDeviceTypeConfiguration implements CoapDeviceTypeConfiguration {

    private static final long serialVersionUID = -8523081152598707064L;

    @NotNull
    @Override
    public CoapDeviceType getCoapDeviceType() {
        return CoapDeviceType.EFENTO;
    }
}
