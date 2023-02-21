package org.echoiot.server.service.security.auth;

import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.RawAccessJwtToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public abstract class AbstractJwtAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -6212297506742428406L;

    @Nullable
    private RawAccessJwtToken rawAccessToken;
    private SecurityUser securityUser;

    public AbstractJwtAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(null);
        this.rawAccessToken = unsafeToken;
        this.setAuthenticated(false);
    }

    public AbstractJwtAuthenticationToken(@NotNull SecurityUser securityUser) {
        super(securityUser.getAuthorities());
        this.eraseCredentials();
        this.securityUser = securityUser;
        super.setAuthenticated(true);
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return rawAccessToken;
    }

    @Override
    public Object getPrincipal() {
        return this.securityUser;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.rawAccessToken = null;
    }
}
