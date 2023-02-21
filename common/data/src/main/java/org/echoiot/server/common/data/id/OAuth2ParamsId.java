package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OAuth2ParamsId extends UUIDBased {

    @JsonCreator
    public OAuth2ParamsId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static OAuth2ParamsId fromString(@NotNull String oauth2ParamsId) {
        return new OAuth2ParamsId(UUID.fromString(oauth2ParamsId));
    }
}
