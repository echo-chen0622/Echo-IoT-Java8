package org.echoiot.server.common.data.security;

import org.jetbrains.annotations.NotNull;

/**
 * @author Valerii Sosliuk
 */
public class DeviceX509Credentials implements DeviceCredentialsFilter {

    private final String sha3Hash;

    public DeviceX509Credentials(String sha3Hash) {
        this.sha3Hash = sha3Hash;
    }

    @Override
    public String getCredentialsId() { return sha3Hash; }

    @NotNull
    @Override
    public DeviceCredentialsType getCredentialsType() { return DeviceCredentialsType.X509_CERTIFICATE; }

    @NotNull
    @Override
    public String toString() {
        return "DeviceX509Credentials [SHA3=" + sha3Hash + "]";
    }
}
