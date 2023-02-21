package org.echoiot.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.oauth2.OAuth2MapperConfig;
import org.echoiot.server.common.data.oauth2.OAuth2Registration;
import org.echoiot.server.dao.oauth2.OAuth2User;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service(value = "basicOAuth2ClientMapper")
@Slf4j
@TbCoreComponent
public class BasicOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(HttpServletRequest request, @NotNull OAuth2AuthenticationToken token, String providerAccessToken, @NotNull OAuth2Registration registration) {
        OAuth2MapperConfig config = registration.getMapperConfig();
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        @Nullable String email = BasicMapperUtils.getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
        @NotNull OAuth2User oauth2User = BasicMapperUtils.getOAuth2User(email, attributes, config);

        return getOrCreateSecurityUserFromOAuth2User(oauth2User, registration);
    }
}
