package org.echoiot.server.dao.device;

import org.echoiot.server.dao.device.provision.ProvisionFailedException;
import org.echoiot.server.dao.device.provision.ProvisionRequest;
import org.echoiot.server.dao.device.provision.ProvisionResponse;

public interface DeviceProvisionService {

    ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) throws ProvisionFailedException;
}
