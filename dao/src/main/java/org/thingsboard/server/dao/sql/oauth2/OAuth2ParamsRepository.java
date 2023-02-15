package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.OAuth2ParamsEntity;

import java.util.UUID;

public interface OAuth2ParamsRepository extends JpaRepository<OAuth2ParamsEntity, UUID> {
}
