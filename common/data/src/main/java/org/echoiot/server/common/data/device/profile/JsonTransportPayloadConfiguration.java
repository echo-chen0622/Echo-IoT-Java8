package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.TransportPayloadType;
import org.jetbrains.annotations.NotNull;

@Data
public class JsonTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    @NotNull
    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.JSON;
    }
}
