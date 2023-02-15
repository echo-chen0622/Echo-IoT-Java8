package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.RequiredArgsConstructor;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;

import java.util.Optional;

@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2mStoreFactory {

    private final Optional<TBRedisCacheConfiguration> redisConfiguration;
    private final LwM2MTransportServerConfig config;
    private final LwM2mCredentialsSecurityInfoValidator validator;

    @Bean
    private CaliforniumRegistrationStore registrationStore() {
        return redisConfiguration.isPresent() ?
                new TbLwM2mRedisRegistrationStore(getConnectionFactory()) : new InMemoryRegistrationStore(config.getCleanPeriodInSec());
    }

    @Bean
    private TbMainSecurityStore securityStore() {
        return new TbLwM2mSecurityStore(redisConfiguration.isPresent() ?
                new TbLwM2mRedisSecurityStore(getConnectionFactory()) : new TbInMemorySecurityStore(), validator);
    }

    @Bean
    private TbLwM2MClientStore clientStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MClientStore(getConnectionFactory()) : new TbDummyLwM2MClientStore();
    }

    @Bean
    private TbLwM2MModelConfigStore modelConfigStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MModelConfigStore(getConnectionFactory()) : new TbDummyLwM2MModelConfigStore();
    }

    @Bean
    private TbLwM2MClientOtaInfoStore otaStore() {
        return redisConfiguration.isPresent() ? new TbLwM2mRedisClientOtaInfoStore(getConnectionFactory()) : new TbDummyLwM2MClientOtaInfoStore();
    }

    @Bean
    private TbLwM2MDtlsSessionStore sessionStore() {
        return redisConfiguration.isPresent() ? new TbLwM2MDtlsSessionRedisStore(getConnectionFactory()) : new TbL2M2MDtlsSessionInMemoryStore();
    }

    private RedisConnectionFactory getConnectionFactory() {
        return redisConfiguration.get().redisConnectionFactory();
    }

}
