package org.echoiot.server.service.security.auth;

import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;

public class RefreshAuthenticationToken extends AbstractJwtAuthenticationToken {

    private static final long serialVersionUID = -1311042791508924523L;

    public RefreshAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(unsafeToken);
    }

    public RefreshAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}
