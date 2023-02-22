package org.echoiot.server.service.security.auth;

import org.echoiot.server.service.security.model.SecurityUser;

public class MfaAuthenticationToken extends AbstractJwtAuthenticationToken {
    public MfaAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}
