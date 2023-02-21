package org.echoiot.server.common.data.oauth2;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.NotNull;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString(exclude = {"password"})
public class OAuth2CustomMapperConfig {
    @NotNull
    @Length(fieldName = "url")
    private final String url;
    @NotNull
    @Length(fieldName = "username")
    private final String username;
    @NotNull
    @Length(fieldName = "password")
    private final String password;
    private final boolean sendToken;
}
