package org.echoiot.server.dao.sql.oauth2;

import org.echoiot.server.dao.model.sql.OAuth2ParamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OAuth2ParamsRepository extends JpaRepository<OAuth2ParamsEntity, UUID> {
}
