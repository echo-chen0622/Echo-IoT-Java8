package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.TransportPayloadType;

@Data
public class JsonTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.JSON;
    }
}
