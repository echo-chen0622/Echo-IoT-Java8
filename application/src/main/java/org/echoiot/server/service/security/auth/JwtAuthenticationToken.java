package org.echoiot.server.service.security.auth;

import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;
import org.jetbrains.annotations.NotNull;

public class JwtAuthenticationToken extends AbstractJwtAuthenticationToken {

    private static final long serialVersionUID = -8487219769037942225L;

    public JwtAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(unsafeToken);
    }

    public JwtAuthenticationToken(@NotNull SecurityUser securityUser) {
        super(securityUser);
    }
}
