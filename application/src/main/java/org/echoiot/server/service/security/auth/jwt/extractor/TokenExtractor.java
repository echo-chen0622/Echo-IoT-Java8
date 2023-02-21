package org.echoiot.server.service.security.auth.jwt.extractor;

import javax.servlet.http.HttpServletRequest;

public interface TokenExtractor {
    String extract(HttpServletRequest request);
}
