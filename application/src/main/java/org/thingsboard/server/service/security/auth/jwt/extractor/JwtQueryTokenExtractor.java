package org.thingsboard.server.service.security.auth.jwt.extractor;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;

import javax.servlet.http.HttpServletRequest;

@Component(value="jwtQueryTokenExtractor")
public class JwtQueryTokenExtractor implements TokenExtractor {

    @Override
    public String extract(HttpServletRequest request) {
        String token = null;
        if (request.getParameterMap() != null && !request.getParameterMap().isEmpty()) {
            String[] tokenParamValue = request.getParameterMap().get(ThingsboardSecurityConfiguration.JWT_TOKEN_QUERY_PARAM);
            if (tokenParamValue != null && tokenParamValue.length == 1) {
                token = tokenParamValue[0];
            }
        }
        if (StringUtils.isBlank(token)) {
            throw new AuthenticationServiceException("Authorization query parameter cannot be blank!");
        }

        return token;
    }
}
