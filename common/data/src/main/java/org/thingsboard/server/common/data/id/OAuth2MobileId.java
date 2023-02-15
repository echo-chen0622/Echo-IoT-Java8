package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class OAuth2MobileId extends UUIDBased {

    @JsonCreator
    public OAuth2MobileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2MobileId fromString(String oauth2MobileId) {
        return new OAuth2MobileId(UUID.fromString(oauth2MobileId));
    }
}
