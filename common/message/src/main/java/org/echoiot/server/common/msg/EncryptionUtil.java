package org.echoiot.server.common.msg;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public class EncryptionUtil {

    private EncryptionUtil() {
    }

    @NotNull
    public static String certTrimNewLines(@NotNull String input) {
        return input.replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END CERTIFICATE-----", "");
    }

    @NotNull
    public static String pubkTrimNewLines(@NotNull String input) {
        return input.replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END PUBLIC KEY-----", "");
    }

    @NotNull
    public static String prikTrimNewLines(@NotNull String input) {
        return input.replaceAll("-----BEGIN EC PRIVATE KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END EC PRIVATE KEY-----", "");
    }


    public static String getSha3Hash(@NotNull String data) {
        @NotNull String trimmedData = certTrimNewLines(data);
        @NotNull byte[] dataBytes = trimmedData.getBytes();
        @NotNull SHA3Digest md = new SHA3Digest(256);
        md.reset();
        md.update(dataBytes, 0, dataBytes.length);
        @NotNull byte[] hashedBytes = new byte[256 / 8];
        md.doFinal(hashedBytes, 0);
        String sha3Hash = ByteUtils.toHexString(hashedBytes);
        return sha3Hash;
    }

    public static String getSha3Hash(String delim, @NotNull String... tokens) {
        @NotNull StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (@Nullable String token : tokens) {
            if (token != null && !token.isEmpty()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(delim);
                }
                sb.append(token);
            }
        }
        return getSha3Hash(sb.toString());
    }
}
