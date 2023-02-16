package org.echoiot.server.dao.device.provision;

public class ProvisionFailedException extends RuntimeException {
    public ProvisionFailedException(String errorMsg) {
        super(errorMsg);
    }
}
