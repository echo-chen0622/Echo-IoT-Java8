package org.thingsboard.server.common.data.device.profile.lwm2m;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryMappingConfiguration implements Serializable {

    private static final long serialVersionUID = -7594999741305410419L;

    private Map<String, String> keyName;
    private Set<String> observe;
    private Set<String> attribute;
    private Set<String> telemetry;
    private Map<String, ObjectAttributes> attributeLwm2m;

}
