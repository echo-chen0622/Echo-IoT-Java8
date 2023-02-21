package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OAuth2RegistrationId extends UUIDBased {

    @JsonCreator
    public OAuth2RegistrationId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static OAuth2RegistrationId fromString(@NotNull String oauth2RegistrationId) {
        return new OAuth2RegistrationId(UUID.fromString(oauth2RegistrationId));
    }
}
