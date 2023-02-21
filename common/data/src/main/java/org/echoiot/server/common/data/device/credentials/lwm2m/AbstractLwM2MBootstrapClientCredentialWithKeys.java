package org.echoiot.server.common.data.device.credentials.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public abstract class AbstractLwM2MBootstrapClientCredentialWithKeys implements LwM2MBootstrapClientCredential {

    private String clientPublicKeyOrId;
    private String clientSecretKey;

    @JsonIgnore
    public byte[] getDecodedClientPublicKeyOrId() {
        return getDecoded(clientPublicKeyOrId);
    }

    @JsonIgnore
    public byte[] getDecodedClientSecretKey() {
        return getDecoded(clientSecretKey);
    }

    @SneakyThrows
    private static byte[] getDecoded(@NotNull String key) {
        return Base64.decodeBase64(key.getBytes());
    }
}
