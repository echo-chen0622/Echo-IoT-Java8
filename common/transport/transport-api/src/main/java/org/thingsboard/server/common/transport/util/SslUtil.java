package org.thingsboard.server.common.transport.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.msg.EncryptionUtil;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public class SslUtil {

    private SslUtil() {
    }

    public static String getCertificateString(Certificate cert)
            throws CertificateEncodingException {
        return EncryptionUtil.certTrimNewLines(Base64Utils.encodeToString(cert.getEncoded()));
    }
}
