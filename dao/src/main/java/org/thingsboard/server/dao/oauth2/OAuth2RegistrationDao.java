package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface OAuth2RegistrationDao extends Dao<OAuth2Registration> {

    List<OAuth2Registration> findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(List<SchemeType> domainSchemes, String domainName, String pkgName, PlatformType platformType);

    List<OAuth2Registration> findByOAuth2ParamsId(UUID oauth2ParamsId);

    String findAppSecret(UUID id, String pkgName);

}
