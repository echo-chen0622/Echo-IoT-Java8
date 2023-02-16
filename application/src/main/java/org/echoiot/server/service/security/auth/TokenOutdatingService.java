package org.echoiot.server.service.security.auth;

import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.model.JwtToken;

public interface TokenOutdatingService {

    boolean isOutdated(JwtToken token, UserId userId);

}
