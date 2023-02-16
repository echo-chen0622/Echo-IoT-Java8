package org.echoiot.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.security.DeviceCredentials;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class DeviceExportData extends EntityExportData<Device> {

    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"id", "deviceId", "createdTime"})
    private DeviceCredentials credentials;

    @JsonIgnore
    @Override
    public boolean hasCredentials() {
        return credentials != null;
    }
}
