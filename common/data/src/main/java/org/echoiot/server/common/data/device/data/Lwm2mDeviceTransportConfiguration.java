package org.echoiot.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class Lwm2mDeviceTransportConfiguration extends PowerSavingConfiguration implements DeviceTransportConfiguration {

    @NotNull
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

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.LWM2M;
    }

}
