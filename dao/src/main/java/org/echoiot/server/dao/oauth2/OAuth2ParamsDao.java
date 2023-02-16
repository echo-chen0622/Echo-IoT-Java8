package org.echoiot.server.dao.oauth2;

import org.echoiot.server.common.data.oauth2.OAuth2Params;
import org.echoiot.server.dao.Dao;

public interface OAuth2ParamsDao extends Dao<OAuth2Params> {
    void deleteAll();
}
