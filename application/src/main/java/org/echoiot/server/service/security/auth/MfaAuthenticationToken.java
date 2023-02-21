package org.echoiot.server.service.security.auth;

import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;

public class MfaAuthenticationToken extends AbstractJwtAuthenticationToken {
    public MfaAuthenticationToken(@NotNull SecurityUser securityUser) {
        super(securityUser);
    }
}
