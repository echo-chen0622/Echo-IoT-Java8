package org.echoiot.server.common.transport.util;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Base64Utils;
import org.echoiot.server.common.msg.EncryptionUtil;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public class SslUtil {

    private SslUtil() {
    }

    @NotNull
    public static String getCertificateString(@NotNull Certificate cert)
            throws CertificateEncodingException {
        return EncryptionUtil.certTrimNewLines(Base64Utils.encodeToString(cert.getEncoded()));
    }
}
