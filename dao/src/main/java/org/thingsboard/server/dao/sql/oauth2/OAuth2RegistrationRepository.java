package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2RegistrationRepository extends JpaRepository<OAuth2RegistrationEntity, UUID> {

    @Query("SELECT reg " +
            "FROM OAuth2RegistrationEntity reg " +
            "LEFT JOIN OAuth2ParamsEntity params on reg.oauth2ParamsId = params.id " +
            "LEFT JOIN OAuth2DomainEntity domain on reg.oauth2ParamsId = domain.oauth2ParamsId " +
            "WHERE params.enabled = true " +
            "AND domain.domainName = :domainName " +
            "AND domain.domainScheme IN (:domainSchemes) " +
            "AND (:pkgName IS NULL OR EXISTS (SELECT mobile FROM OAuth2MobileEntity mobile WHERE mobile.oauth2ParamsId = reg.oauth2ParamsId AND mobile.pkgName = :pkgName)) " +
            "AND (:platformFilter IS NULL OR reg.platforms IS NULL OR reg.platforms = '' OR reg.platforms LIKE :platformFilter)")
    List<OAuth2RegistrationEntity> findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(@Param("domainSchemes") List<SchemeType> domainSchemes,
                                                                                                 @Param("domainName") String domainName,
                                                                                                 @Param("pkgName") String pkgName,
                                                                                                 @Param("platformFilter") String platformFilter);

    List<OAuth2RegistrationEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

    @Query("SELECT mobile.appSecret " +
            "FROM OAuth2MobileEntity mobile " +
            "LEFT JOIN OAuth2RegistrationEntity reg on mobile.oauth2ParamsId = reg.oauth2ParamsId " +
            "WHERE reg.id = :registrationId " +
            "AND mobile.pkgName = :pkgName")
    String findAppSecret(@Param("registrationId") UUID id,
                         @Param("pkgName") String pkgName);

}
