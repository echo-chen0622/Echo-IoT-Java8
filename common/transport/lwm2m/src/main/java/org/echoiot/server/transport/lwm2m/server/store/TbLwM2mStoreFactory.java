package org.echoiot.server.transport.lwm2m.server.store;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2mStoreFactory {

    @NotNull
    private final Optional<TBRedisCacheConfiguration> redisConfiguration;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2mCredentialsSecurityInfoValidator validator;

    @NotNull
    @Bean
    private CaliforniumRegistrationStore registrationStore() {
        return redisConfiguration.isPresent() ?
                new TbLwM2mRedisRegistrationStore(getConnectionFactory()) : new InMemoryRegistrationStore(config.getCleanPeriodInSec());
    }

    @NotNull
    @Bean
    private TbMainSecurityStore securityStore() {
        return new TbLwM2mSecurityStore(redisConfiguration.isPresent() ?
                new TbLwM2mRedisSecurityStore(getConnectionFactory()) : new TbInMemorySecurityStore(), validator);
    }

    @NotNull
    @Bean
    private TbLwM2MClientStore clientStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MClientStore(getConnectionFactory()) : new TbDummyLwM2MClientStore();
    }

    @NotNull
    @Bean
    private TbLwM2MModelConfigStore modelConfigStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MModelConfigStore(getConnectionFactory()) : new TbDummyLwM2MModelConfigStore();
    }

    @NotNull
    @Bean
    private TbLwM2MClientOtaInfoStore otaStore() {
        return redisConfiguration.isPresent() ? new TbLwM2mRedisClientOtaInfoStore(getConnectionFactory()) : new TbDummyLwM2MClientOtaInfoStore();
    }

    @NotNull
    @Bean
    private TbLwM2MDtlsSessionStore sessionStore() {
        return redisConfiguration.isPresent() ? new TbLwM2MDtlsSessionRedisStore(getConnectionFactory()) : new TbL2M2MDtlsSessionInMemoryStore();
    }

    private RedisConnectionFactory getConnectionFactory() {
        return redisConfiguration.get().redisConnectionFactory();
    }

}
