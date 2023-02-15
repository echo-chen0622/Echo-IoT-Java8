package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;

@Slf4j
public class FSTUtils {

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    @SuppressWarnings("unchecked")
    public static <T> T decode(byte[] byteArray) {
        return byteArray != null && byteArray.length > 0 ? (T) CONFIG.asObject(byteArray) : null;
    }

    public static <T> byte[] encode(T msq) {
        return CONFIG.asByteArray(msq);
    }

}
