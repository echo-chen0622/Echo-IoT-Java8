package org.echoiot.server.service.security.auth.oauth2;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.oauth2.OAuth2MapperConfig;
import org.echoiot.server.common.data.oauth2.OAuth2Registration;
import org.echoiot.server.dao.oauth2.OAuth2Configuration;
import org.echoiot.server.dao.oauth2.OAuth2User;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.echoiot.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Service(value = "githubOAuth2ClientMapper")
@Slf4j
@TbCoreComponent
public class GithubOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {
    private static final String EMAIL_URL_KEY = "emailUrl";

    private static final String AUTHORIZATION = "Authorization";

    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @Resource
    private OAuth2Configuration oAuth2Configuration;

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(HttpServletRequest request, @NotNull OAuth2AuthenticationToken token, String providerAccessToken, @NotNull OAuth2Registration registration) {
        OAuth2MapperConfig config = registration.getMapperConfig();
        Map<String, String> githubMapperConfig = oAuth2Configuration.getGithubMapper();
        @NotNull String email = getEmail(githubMapperConfig.get(EMAIL_URL_KEY), providerAccessToken);
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        @NotNull OAuth2User oAuth2User = BasicMapperUtils.getOAuth2User(email, attributes, config);
        return getOrCreateSecurityUserFromOAuth2User(oAuth2User, registration);
    }

    @NotNull
    private synchronized String getEmail(@NotNull String emailUrl, String oauth2Token) {
        restTemplateBuilder = restTemplateBuilder.defaultHeader(AUTHORIZATION, "token " + oauth2Token);

        RestTemplate restTemplate = restTemplateBuilder.build();
        @Nullable GithubEmailsResponse githubEmailsResponse;
        try {
            githubEmailsResponse = restTemplate.getForEntity(emailUrl, GithubEmailsResponse.class).getBody();
            if (githubEmailsResponse == null){
                throw new RuntimeException("Empty Github response!");
            }
        } catch (Exception e) {
            log.error("There was an error during connection to Github API", e);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
        @NotNull Optional<String> emailOpt = githubEmailsResponse.stream()
                                                                 .filter(GithubEmailResponse::isPrimary)
                                                                 .map(GithubEmailResponse::getEmail)
                                                                 .findAny();
        if (emailOpt.isPresent()){
            return emailOpt.get();
        } else {
            log.error("Could not find primary email from {}.", githubEmailsResponse);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
    }
    private static class GithubEmailsResponse extends ArrayList<GithubEmailResponse> {}

    @Data
    @ToString
    private static class GithubEmailResponse {
        private String email;
        private boolean verified;
        private boolean primary;
        private String visibility;
    }
}
