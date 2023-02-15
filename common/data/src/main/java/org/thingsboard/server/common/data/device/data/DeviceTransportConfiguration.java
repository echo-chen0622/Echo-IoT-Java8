package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import org.thingsboard.server.common.data.DeviceTransportType;

import java.io.Serializable;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDeviceTransportConfiguration.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = MqttDeviceTransportConfiguration.class, name = "MQTT"),
        @JsonSubTypes.Type(value = CoapDeviceTransportConfiguration.class, name = "COAP"),
        @JsonSubTypes.Type(value = Lwm2mDeviceTransportConfiguration.class, name = "LWM2M"),
        @JsonSubTypes.Type(value = SnmpDeviceTransportConfiguration.class, name = "SNMP")})
public interface DeviceTransportConfiguration extends Serializable {
    @JsonIgnore
    DeviceTransportType getType();

    default void validate() {
    }

}
