package org.echoiot.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnonymousCredentials implements ClientCredentials {
    @NotNull
    @Override
    public CredentialsType getType() {
        return CredentialsType.ANONYMOUS;
    }
}
