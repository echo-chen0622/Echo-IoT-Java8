package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicCredentials implements ClientCredentials {
    private String username;
    private String password;

    @Override
    public CredentialsType getType() {
        return CredentialsType.BASIC;
    }
}
