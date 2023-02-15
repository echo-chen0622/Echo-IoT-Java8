package org.thingsboard.server.dao.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;

import java.util.UUID;

@Component
public class HybridClientRegistrationRepository implements ClientRegistrationRepository {
    private static final String defaultRedirectUriTemplate = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Autowired
    private OAuth2Service oAuth2Service;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        OAuth2Registration registration = oAuth2Service.findRegistration(UUID.fromString(registrationId));
        return registration == null ?
                null : toSpringClientRegistration(registration);
    }

    private ClientRegistration toSpringClientRegistration(OAuth2Registration registration){
        String registrationId = registration.getUuidId().toString();
        return ClientRegistration.withRegistrationId(registrationId)
                .clientName(registration.getName())
                .clientId(registration.getClientId())
                .authorizationUri(registration.getAuthorizationUri())
                .clientSecret(registration.getClientSecret())
                .tokenUri(registration.getAccessTokenUri())
                .scope(registration.getScope())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .userInfoUri(registration.getUserInfoUri())
                .userNameAttributeName(registration.getUserNameAttributeName())
                .jwkSetUri(registration.getJwkSetUri())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(registration.getClientAuthenticationMethod()))
                .redirectUri(defaultRedirectUriTemplate)
                .build();
    }
}
