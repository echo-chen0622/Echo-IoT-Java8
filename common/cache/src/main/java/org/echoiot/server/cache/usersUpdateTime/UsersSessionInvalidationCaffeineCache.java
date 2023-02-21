package org.echoiot.server.cache.usersUpdateTime;

import org.echoiot.server.cache.CaffeineTbTransactionalCache;
import org.echoiot.server.common.data.CacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;


@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("UsersSessionInvalidation")
public class UsersSessionInvalidationCaffeineCache extends CaffeineTbTransactionalCache<String, Long> {

    /**
     * 这里需要用的哦啊 @Autowired 注解，因为这里的构造函数是有参数的，需要 spring boot 自动装配
     * 而父类 CaffeineTbTransactionalCache 有 @RequiredArgsConstructor 注解，为了避免 cacheManager 被父类覆盖，所以这里需要用 @Autowired 注解
     * 一般情况下，spring 自动注入，是不需要用 @Autowired 注解的
     *
     * @param cacheManager 缓存管理器
     */
    @Autowired
    public UsersSessionInvalidationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.USERS_SESSION_INVALIDATION_CACHE);
    }
}
