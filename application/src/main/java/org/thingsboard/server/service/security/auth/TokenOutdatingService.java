package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.JwtToken;

public interface TokenOutdatingService {

    boolean isOutdated(JwtToken token, UserId userId);

}
