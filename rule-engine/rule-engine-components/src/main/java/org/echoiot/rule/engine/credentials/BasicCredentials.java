package org.echoiot.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicCredentials implements ClientCredentials {
    private String username;
    private String password;

    @NotNull
    @Override
    public CredentialsType getType() {
        return CredentialsType.BASIC;
    }
}
