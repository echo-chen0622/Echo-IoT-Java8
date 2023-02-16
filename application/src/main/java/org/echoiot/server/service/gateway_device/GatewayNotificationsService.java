package org.echoiot.server.service.gateway_device;

import org.echoiot.server.common.data.Device;

public interface GatewayNotificationsService {

    void onDeviceUpdated(Device device, Device oldDevice);

    void onDeviceDeleted(Device device);
}
