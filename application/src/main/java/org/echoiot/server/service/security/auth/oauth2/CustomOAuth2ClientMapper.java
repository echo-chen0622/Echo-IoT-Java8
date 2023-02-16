package org.echoiot.server.service.security.auth.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.echoiot.server.common.data.oauth2.OAuth2MapperConfig;
import org.echoiot.server.common.data.oauth2.OAuth2Registration;
import org.echoiot.server.dao.oauth2.OAuth2User;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.echoiot.server.service.security.model.SecurityUser;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

@Service(value = "customOAuth2ClientMapper")
@Slf4j
@TbCoreComponent
public class CustomOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {
    private static final String PROVIDER_ACCESS_TOKEN = "provider-access-token";

    private static final ObjectMapper json = new ObjectMapper();

    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @PostConstruct
    public void init() {
        // Register time module to parse Instant objects.
        // com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
        // Java 8 date/time type `java.time.Instant` not supported by default:
        // add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
        json.registerModule(new JavaTimeModule());
    }

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(HttpServletRequest request, OAuth2AuthenticationToken token, String providerAccessToken, OAuth2Registration registration) {
        OAuth2MapperConfig config = registration.getMapperConfig();
        OAuth2User oauth2User = getOAuth2User(token, providerAccessToken, config.getCustom());
        return getOrCreateSecurityUserFromOAuth2User(oauth2User, registration);
    }

    private synchronized OAuth2User getOAuth2User(OAuth2AuthenticationToken token, String providerAccessToken, OAuth2CustomMapperConfig custom) {
        if (!StringUtils.isEmpty(custom.getUsername()) && !StringUtils.isEmpty(custom.getPassword())) {
            restTemplateBuilder = restTemplateBuilder.basicAuthentication(custom.getUsername(), custom.getPassword());
        }
        if (custom.isSendToken() && !StringUtils.isEmpty(providerAccessToken)) {
            restTemplateBuilder = restTemplateBuilder.defaultHeader(PROVIDER_ACCESS_TOKEN, providerAccessToken);
        }

        RestTemplate restTemplate = restTemplateBuilder.build();
        String request;
        try {
            request = json.writeValueAsString(token.getPrincipal());
        } catch (JsonProcessingException e) {
            log.error("Can't convert principal to JSON string", e);
            throw new RuntimeException("Can't convert principal to JSON string", e);
        }
        try {
            return restTemplate.postForEntity(custom.getUrl(), request, OAuth2User.class).getBody();
        } catch (Exception e) {
            log.error("There was an error during connection to custom mapper endpoint", e);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
    }
}