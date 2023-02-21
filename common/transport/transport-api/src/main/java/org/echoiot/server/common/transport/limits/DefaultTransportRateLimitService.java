package org.echoiot.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.transport.TransportTenantProfileCache;
import org.echoiot.server.common.transport.profile.TenantProfileUpdateResult;
import org.echoiot.server.queue.util.TbTransportComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@TbTransportComponent
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final static DummyTransportRateLimit ALLOW = new DummyTransportRateLimit();
    private final ConcurrentMap<TenantId, Boolean> tenantAllowed = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, EntityTransportRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perDeviceLimits = new ConcurrentHashMap<>();
    private final Map<InetAddress, InetAddressRateLimitStats> ipMap = new ConcurrentHashMap<>();

    private final TransportTenantProfileCache tenantProfileCache;

    @Value("${transport.rate_limits.ip_limits_enabled:false}")
    private boolean ipRateLimitsEnabled;
    @Value("${transport.rate_limits.max_wrong_credentials_per_ip:10}")
    private int maxWrongCredentialsPerIp;
    @Value("${transport.rate_limits.ip_block_timeout:60000}")
    private long ipBlockTimeout;

    public DefaultTransportRateLimitService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Nullable
    @Override
    public EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return EntityType.API_USAGE_STATE;
        }
        if (!checkEntityRateLimit(dataPoints, getTenantRateLimits(tenantId))) {
            return EntityType.TENANT;
        }
        if (!checkEntityRateLimit(dataPoints, getDeviceRateLimits(tenantId, deviceId))) {
            return EntityType.DEVICE;
        }
        return null;
    }

    private boolean checkEntityRateLimit(int dataPoints, @NotNull EntityTransportRateLimits tenantLimits) {
        if (dataPoints > 0) {
            return tenantLimits.getTelemetryMsgRateLimit().tryConsume() && tenantLimits.getTelemetryDataPointsRateLimit().tryConsume(dataPoints);
        } else {
            return tenantLimits.getRegularMsgRateLimit().tryConsume();
        }
    }

    @Override
    public void update(@NotNull TenantProfileUpdateResult update) {
        log.info("Received tenant profile update: {}", update.getProfile());
        @NotNull EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(update.getProfile(), true);
        @NotNull EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(update.getProfile(), false);
        for (@NotNull TenantId tenantId : update.getAffectedTenants()) {
            mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
            tenantDevices.get(tenantId).forEach(deviceId -> {
                mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
            });
        }
    }

    @Override
    public void update(@NotNull TenantId tenantId) {
        @NotNull EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), true);
        @NotNull EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), false);
        mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
        tenantDevices.get(tenantId).forEach(deviceId -> {
            mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
        });
    }

    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
        tenantDevices.remove(tenantId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
        tenantDevices.values().forEach(set -> set.remove(deviceId));
    }

    @Override
    public void update(TenantId tenantId, boolean allowed) {
        tenantAllowed.put(tenantId, allowed);
    }

    @Override
    public boolean checkAddress(@NotNull InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return true;
        }
        @NotNull var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        return !stats.isBlocked() || (stats.getLastActivityTs() + ipBlockTimeout < System.currentTimeMillis());
    }

    @Override
    public void onAuthSuccess(@NotNull InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        @NotNull var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            stats.setFailureCount(0);
            if (stats.isBlocked()) {
                stats.setBlocked(false);
                log.info("[{}] IP address un-blocked due to correct credentials.", address.getAddress());
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void onAuthFailure(@NotNull InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        @NotNull var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            int failureCount = stats.getFailureCount() + 1;
            stats.setFailureCount(failureCount);
            if (failureCount >= maxWrongCredentialsPerIp) {
                log.info("[{}] IP address blocked due to constantly wrong credentials.", address.getAddress());
                stats.setBlocked(true);
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void invalidateRateLimitsIpTable(long sessionInactivityTimeout) {
        if (!ipRateLimitsEnabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long expTime = currentTime - Math.max(sessionInactivityTimeout, ipBlockTimeout);
        for (@NotNull var entry : ipMap.entrySet()) {
            var stats = entry.getValue();
            if (stats.getLastActivityTs() < expTime) {
                log.debug("[{}] IP address removed due to session inactivity timeout.", entry.getKey());
                ipMap.remove(entry.getKey());
            } else if (stats.isBlocked() && (stats.getLastActivityTs() + ipBlockTimeout < currentTime)) {
                log.info("[{}] IP address unblocked due ip block timeout.", entry.getKey());
                stats.setBlocked(false);
            }
        }
    }

    private <T extends EntityId> void mergeLimits(@NotNull T entityId, @NotNull EntityTransportRateLimits newRateLimits,
                                                  @NotNull Function<T, EntityTransportRateLimits> getFunction,
                                                  @NotNull BiConsumer<T, EntityTransportRateLimits> putFunction) {
        EntityTransportRateLimits oldRateLimits = getFunction.apply(entityId);
        if (oldRateLimits == null) {
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                log.info("[{}] New rate limits: {}", entityId, newRateLimits);
            } else {
                log.debug("[{}] New rate limits: {}", entityId, newRateLimits);
            }
            putFunction.accept(entityId, newRateLimits);
        } else {
            @Nullable EntityTransportRateLimits updated = merge(oldRateLimits, newRateLimits);
            if (updated != null) {
                if (EntityType.TENANT.equals(entityId.getEntityType())) {
                    log.info("[{}] Updated rate limits: {}", entityId, updated);
                } else {
                    log.debug("[{}] Updated rate limits: {}", entityId, updated);
                }
                putFunction.accept(entityId, updated);
            }
        }
    }

    @Nullable
    private EntityTransportRateLimits merge(@NotNull EntityTransportRateLimits oldRateLimits, @NotNull EntityTransportRateLimits newRateLimits) {
        boolean regularUpdate = !oldRateLimits.getRegularMsgRateLimit().getConfiguration().equals(newRateLimits.getRegularMsgRateLimit().getConfiguration());
        boolean telemetryMsgRateUpdate = !oldRateLimits.getTelemetryMsgRateLimit().getConfiguration().equals(newRateLimits.getTelemetryMsgRateLimit().getConfiguration());
        boolean telemetryDataPointUpdate = !oldRateLimits.getTelemetryDataPointsRateLimit().getConfiguration().equals(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration());
        if (regularUpdate || telemetryMsgRateUpdate || telemetryDataPointUpdate) {
            return new EntityTransportRateLimits(
                    regularUpdate ? newLimit(newRateLimits.getRegularMsgRateLimit().getConfiguration()) : oldRateLimits.getRegularMsgRateLimit(),
                    telemetryMsgRateUpdate ? newLimit(newRateLimits.getTelemetryMsgRateLimit().getConfiguration()) : oldRateLimits.getTelemetryMsgRateLimit(),
                    telemetryDataPointUpdate ? newLimit(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration()) : oldRateLimits.getTelemetryDataPointsRateLimit());
        } else {
            return null;
        }
    }

    @NotNull
    private EntityTransportRateLimits createRateLimits(@NotNull TenantProfile tenantProfile, boolean tenant) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        DefaultTenantProfileConfiguration profile = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
        if (profile == null) {
            return new EntityTransportRateLimits(ALLOW, ALLOW, ALLOW);
        } else {
            @NotNull TransportRateLimit regularMsgRateLimit = newLimit(tenant ? profile.getTransportTenantMsgRateLimit() : profile.getTransportDeviceMsgRateLimit());
            @NotNull TransportRateLimit telemetryMsgRateLimit = newLimit(tenant ? profile.getTransportTenantTelemetryMsgRateLimit() : profile.getTransportDeviceTelemetryMsgRateLimit());
            @NotNull TransportRateLimit telemetryDpRateLimit = newLimit(profile.getTransportTenantTelemetryDataPointsRateLimit());
            return new EntityTransportRateLimits(regularMsgRateLimit, telemetryMsgRateLimit, telemetryDpRateLimit);
        }
    }

    @NotNull
    private static TransportRateLimit newLimit(@NotNull String config) {
        return StringUtils.isEmpty(config) ? ALLOW : new SimpleTransportRateLimit(config);
    }

    @NotNull
    private EntityTransportRateLimits getTenantRateLimits(TenantId tenantId) {
        EntityTransportRateLimits limits = perTenantLimits.get(tenantId);
        if (limits == null) {
            limits = createRateLimits(tenantProfileCache.get(tenantId), true);
            perTenantLimits.put(tenantId, limits);
        }
        return limits;
    }

    @NotNull
    private EntityTransportRateLimits getDeviceRateLimits(TenantId tenantId, DeviceId deviceId) {
        EntityTransportRateLimits limits = perDeviceLimits.get(deviceId);
        if (limits == null) {
            limits = createRateLimits(tenantProfileCache.get(tenantId), false);
            perDeviceLimits.put(deviceId, limits);
            tenantDevices.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet()).add(deviceId);
        }
        return limits;
    }
}
