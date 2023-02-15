package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface OAuth2MobileDao extends Dao<OAuth2Mobile> {

    List<OAuth2Mobile> findByOAuth2ParamsId(UUID oauth2ParamsId);

}
