package org.echoiot.server.service.security.auth.jwt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RefreshTokenRequest {
    private final String refreshToken;

    @JsonCreator
    public RefreshTokenRequest(@JsonProperty("refreshToken") String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
