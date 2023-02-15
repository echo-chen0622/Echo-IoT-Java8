package org.thingsboard.server.dao.device;

import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;

public interface DeviceProvisionService {

    ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) throws ProvisionFailedException;
}
