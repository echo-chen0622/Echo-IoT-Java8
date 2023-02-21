package org.echoiot.server.common.data.security;

import org.jetbrains.annotations.NotNull;

public class DeviceTokenCredentials implements DeviceCredentialsFilter {

    private final String token;

    public DeviceTokenCredentials(String token) {
        this.token = token;
    }

    @NotNull
    @Override
    public DeviceCredentialsType getCredentialsType() {
        return DeviceCredentialsType.ACCESS_TOKEN;
    }

    @Override
    public String getCredentialsId() {
        return token;
    }

    @NotNull
    @Override
    public String toString() {
        return "DeviceTokenCredentials [token=" + token + "]";
    }

}
