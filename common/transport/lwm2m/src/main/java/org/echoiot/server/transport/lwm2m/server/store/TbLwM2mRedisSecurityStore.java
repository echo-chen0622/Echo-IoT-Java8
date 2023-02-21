package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.concurrent.locks.Lock;

public class TbLwM2mRedisSecurityStore implements TbEditableSecurityStore {
    private static final String SEC_EP = "SEC#EP#";
    private static final String LOCK_EP = "LOCK#EP#";
    private static final String PSKID_SEC = "PSKID#SEC";

    private final RedisConnectionFactory connectionFactory;
    private final FSTConfiguration serializer;
    @NotNull
    private final RedisLockRegistry redisLock;

    public TbLwM2mRedisSecurityStore(@NotNull RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        redisLock = new RedisLockRegistry(connectionFactory, "Security");
        serializer = FSTConfiguration.createDefaultConfiguration();
    }

    @Nullable
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        @Nullable Lock lock = null;
        try (@NotNull var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(endpoint));
            lock.lock();
            @Nullable byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data == null || data.length == 0) {
                return null;
            } else {
                if (SecurityMode.NO_SEC.equals(((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityMode())) {
                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                }
                else {
                    return ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Nullable
    @Override
    public SecurityInfo getByIdentity(@NotNull String identity) {
        @Nullable Lock lock = null;
        try (@NotNull var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(identity));
            lock.lock();
            @Nullable byte[] ep = connection.hGet(PSKID_SEC.getBytes(), identity.getBytes());
            if (ep == null) {
                return null;
            } else {
                @Nullable byte[] data = connection.get((SEC_EP + new String(ep)).getBytes());
                if (data == null || data.length == 0) {
                    return null;
                } else {
                    return ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void put(@NotNull TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        SecurityInfo info = tbSecurityInfo.getSecurityInfo();
        byte[] tbSecurityInfoSerialized = serializer.asByteArray(tbSecurityInfo);
        @Nullable Lock lock = null;
        try (@NotNull var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(tbSecurityInfo.getEndpoint());
            lock.lock();
            if (info != null && info.getIdentity() != null) {
                @Nullable byte[] oldEndpointBytes = connection.hGet(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                if (oldEndpointBytes != null) {
                    @NotNull String oldEndpoint = new String(oldEndpointBytes);
                    if (!oldEndpoint.equals(info.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
                    }
                    connection.hSet(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
                }
            }

            @Nullable byte[] previousData = connection.getSet((SEC_EP + tbSecurityInfo.getEndpoint()).getBytes(), tbSecurityInfoSerialized);
            if (previousData != null && info != null) {
                String previousIdentity = ((TbLwM2MSecurityInfo) serializer.asObject(previousData)).getSecurityInfo().getIdentity();
                if (previousIdentity != null && !previousIdentity.equals(info.getIdentity())) {
                    connection.hDel(PSKID_SEC.getBytes(), previousIdentity.getBytes());
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Nullable
    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        @Nullable Lock lock = null;
        try (@NotNull var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            @Nullable byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                return (TbLwM2MSecurityInfo) serializer.asObject(data);
            } else {
                return null;
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        @Nullable Lock lock = null;
        try (@NotNull var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            @Nullable byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                SecurityInfo info = ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                if (info != null && info.getIdentity() != null) {
                    connection.hDel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                }
                connection.del((SEC_EP + endpoint).getBytes());
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @NotNull
    private String toLockKey(String endpoint) {
        return LOCK_EP + endpoint;
    }
}
