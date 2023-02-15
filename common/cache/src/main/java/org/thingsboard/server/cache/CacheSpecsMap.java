package org.thingsboard.server.cache;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.CacheConstants;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "cache")
@Data
public class CacheSpecsMap {

    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private int refreshTokenExpTime;

    @Getter
    private Map<String, CacheSpecs> specs;

    @PostConstruct
    public void replaceTheJWTTokenRefreshExpTime() {
        if (specs != null) {
            var cacheSpecs = specs.get(CacheConstants.USERS_SESSION_INVALIDATION_CACHE);
            if (cacheSpecs != null) {
                cacheSpecs.setTimeToLiveInMinutes((refreshTokenExpTime / 60) + 1);
            }
        }
    }

}
