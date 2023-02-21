package org.echoiot.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class PSKClientCredential extends AbstractLwM2MClientSecurityCredential {
    private String identity;

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.PSK;
    }

    @Override
    public byte[] getDecoded() throws IllegalArgumentException, DecoderException {
        if (securityInBytes == null) {
                securityInBytes = Hex.decodeHex(key.toLowerCase().toCharArray());
        }
        return securityInBytes;
    }
}
