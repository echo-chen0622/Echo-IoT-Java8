package org.echoiot.server.service.security.auth;

import io.jsonwebtoken.Claims;
import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.event.UserAuthDataChangedEvent;
import org.echoiot.server.common.data.security.model.JwtToken;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class DefaultTokenOutdatingService implements TokenOutdatingService {

    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    public DefaultTokenOutdatingService(@Qualifier("UsersSessionInvalidation") TbTransactionalCache<String, Long> cache, JwtTokenFactory tokenFactory) {
        this.cache = cache;
        this.tokenFactory = tokenFactory;
    }

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(@NotNull UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    @Override
    public boolean isOutdated(@NotNull JwtToken token, @NotNull UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class);
        if (isTokenOutdated(issueTime, userId.toString())){
             return true;
        } else {
             return sessionId != null && isTokenOutdated(issueTime, sessionId);
        }
    }

    @NotNull
    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatageTime -> isTokenOutdated(issueTime, outdatageTime.get())).orElse(false);
    }

    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }
}
