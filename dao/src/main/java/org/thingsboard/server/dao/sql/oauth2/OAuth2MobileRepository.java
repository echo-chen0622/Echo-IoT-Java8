package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.OAuth2MobileEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2MobileRepository extends JpaRepository<OAuth2MobileEntity, UUID> {

    List<OAuth2MobileEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

}
