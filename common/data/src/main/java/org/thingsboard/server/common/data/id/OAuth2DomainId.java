package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class OAuth2DomainId extends UUIDBased {

    @JsonCreator
    public OAuth2DomainId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2DomainId fromString(String oauth2DomainId) {
        return new OAuth2DomainId(UUID.fromString(oauth2DomainId));
    }
}
