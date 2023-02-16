package org.echoiot.server.common.transport.auth;

import lombok.Data;
import org.echoiot.server.common.data.device.data.PowerMode;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class TransportDeviceInfo implements Serializable {

    private TenantId tenantId;
    private CustomerId customerId;
    private DeviceProfileId deviceProfileId;
    private DeviceId deviceId;
    private String deviceName;
    private String deviceType;
    private PowerMode powerMode;
    private String additionalInfo;
    private Long edrxCycle;
    private Long psmActivityTimer;
    private Long pagingTransmissionWindow;
}
