package org.echoiot.server.service.security.model.token;

import org.echoiot.server.common.data.security.model.JwtToken;

import java.io.Serializable;

public class RawAccessJwtToken implements JwtToken, Serializable {

    private static final long serialVersionUID = -797397445703066079L;

    private final String token;

    public RawAccessJwtToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }
}
