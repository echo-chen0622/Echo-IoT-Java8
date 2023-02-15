package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;

import java.util.HashMap;
import java.util.Map;

@Data
public class CoapDeviceTransportConfiguration extends PowerSavingConfiguration implements DeviceTransportConfiguration {

    private static final long serialVersionUID = 6061442236008925609L;

    @JsonIgnore
    private Map<String, Object> properties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> properties() {
        return this.properties;
    }

    @JsonAnySetter
    public void put(String name, Object value) {
        this.properties.put(name, value);
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.COAP;
    }

}
