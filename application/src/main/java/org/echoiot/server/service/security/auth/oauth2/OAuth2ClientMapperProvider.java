package org.echoiot.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.oauth2.MapperType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
@TbCoreComponent
public class OAuth2ClientMapperProvider {

    @Resource(name = "basicOAuth2ClientMapper")
    private OAuth2ClientMapper basicOAuth2ClientMapper;

    @Resource(name = "customOAuth2ClientMapper")
    private OAuth2ClientMapper customOAuth2ClientMapper;

    @Resource(name = "githubOAuth2ClientMapper")
    private OAuth2ClientMapper githubOAuth2ClientMapper;

    @Resource(name = "appleOAuth2ClientMapper")
    private OAuth2ClientMapper appleOAuth2ClientMapper;

    public OAuth2ClientMapper getOAuth2ClientMapperByType(MapperType oauth2MapperType) {
        switch (oauth2MapperType) {
            case CUSTOM:
                return customOAuth2ClientMapper;
            case BASIC:
                return basicOAuth2ClientMapper;
            case GITHUB:
                return githubOAuth2ClientMapper;
            case APPLE:
                return appleOAuth2ClientMapper;
            default:
                throw new RuntimeException("OAuth2ClientRegistrationMapper with type " + oauth2MapperType + " is not supported!");
        }
    }
}
