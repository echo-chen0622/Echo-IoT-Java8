package org.echoiot.server.dao.user;

import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserAuthSettings;
import org.echoiot.server.dao.Dao;

public interface UserAuthSettingsDao extends Dao<UserAuthSettings> {

    UserAuthSettings findByUserId(UserId userId);

    void removeByUserId(UserId userId);

}
