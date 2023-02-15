package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.DecoderException;

public abstract class AbstractLwM2MClientSecurityCredential extends AbstractLwM2MClientCredential {
    @Getter
    @Setter
    protected String key;

    protected byte[] securityInBytes;

    public abstract byte[] getDecoded() throws IllegalArgumentException, DecoderException;
}
