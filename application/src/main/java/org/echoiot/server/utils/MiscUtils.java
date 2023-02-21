package org.echoiot.server.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * @author Andrew Shvayka
 */
public class MiscUtils {

    public static final Charset UTF8 = StandardCharsets.UTF_8;

    @NotNull
    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public static HashFunction forName(@NotNull String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    public static String constructBaseUrl(@NotNull HttpServletRequest request) {
        return String.format("%s://%s:%d",
                getScheme(request),
                getDomainName(request),
                getPort(request));
    }

    public static String getScheme(@NotNull HttpServletRequest request){
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("x-forwarded-proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        return scheme;
    }

    public static String getDomainName(@NotNull HttpServletRequest request){
        return request.getServerName();
    }

    public static String getDomainNameAndPort(@NotNull HttpServletRequest request){
        String domainName = getDomainName(request);
        String scheme = getScheme(request);
        int port = MiscUtils.getPort(request);
        if (needsPort(scheme, port)) {
            domainName += ":" + port;
        }
        return domainName;
    }

    private static boolean needsPort(@NotNull String scheme, int port) {
        boolean isHttpDefault = "http".equalsIgnoreCase(scheme) && port == 80;
        boolean isHttpsDefault = "https".equalsIgnoreCase(scheme) && port == 443;
        return !isHttpDefault && !isHttpsDefault;
    }

    public static int getPort(@NotNull HttpServletRequest request){
        String forwardedProto = request.getHeader("x-forwarded-proto");

        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        } else if (forwardedProto != null) {
            switch (forwardedProto) {
                case "http":
                    serverPort = 80;
                    break;
                case "https":
                    serverPort = 443;
                    break;
            }
        }
        return serverPort;
    }
}
