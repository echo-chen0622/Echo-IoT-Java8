package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface OAuth2DomainDao extends Dao<OAuth2Domain> {

    List<OAuth2Domain> findByOAuth2ParamsId(UUID oauth2ParamsId);

}
