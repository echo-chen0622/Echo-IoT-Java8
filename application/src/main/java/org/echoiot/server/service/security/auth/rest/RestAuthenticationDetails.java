package org.echoiot.server.service.security.auth.rest;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ua_parser.Client;
import ua_parser.Parser;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;

@Data
public class RestAuthenticationDetails implements Serializable {

    private final String clientAddress;
    private final Client userAgent;

    public RestAuthenticationDetails(@NotNull HttpServletRequest request) {
        this.clientAddress = getClientIP(request);
        this.userAgent = getUserAgent(request);
    }

    private static String getClientIP(@NotNull HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private static Client getUserAgent(@NotNull HttpServletRequest request) {
        try {
            @NotNull Parser uaParser = new Parser();
            return uaParser.parse(request.getHeader("User-Agent"));
        } catch (IOException e) {
            return new Client(null, null, null);
        }
    }
}
