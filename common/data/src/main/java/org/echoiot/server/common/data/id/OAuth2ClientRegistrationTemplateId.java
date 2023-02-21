package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OAuth2ClientRegistrationTemplateId extends UUIDBased {

    @JsonCreator
    public OAuth2ClientRegistrationTemplateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static OAuth2ClientRegistrationTemplateId fromString(@NotNull String clientRegistrationTemplateId) {
        return new OAuth2ClientRegistrationTemplateId(UUID.fromString(clientRegistrationTemplateId));
    }
}
