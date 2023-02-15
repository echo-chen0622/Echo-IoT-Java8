package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import java.util.List;
import java.util.UUID;

public interface OAuth2Service {
    List<OAuth2ClientInfo> getOAuth2Clients(String domainScheme, String domainName, String pkgName, PlatformType platformType);

    void saveOAuth2Info(OAuth2Info oauth2Info);

    OAuth2Info findOAuth2Info();

    OAuth2Registration findRegistration(UUID id);

    List<OAuth2Registration> findAllRegistrations();

    String findAppSecret(UUID registrationId, String pkgName);
}
