package org.echoiot.server.common.transport.service;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.TransportTenantProfileCache;
import org.echoiot.server.common.transport.limits.TransportRateLimitService;
import org.echoiot.server.common.transport.profile.TenantProfileUpdateResult;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbTransportComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@TbTransportComponent
@Slf4j
public class DefaultTransportTenantProfileCache implements TransportTenantProfileCache {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantProfileId, Set<TenantId>> tenantProfileIds = new ConcurrentHashMap<>();
    private final DataDecodingEncodingService dataDecodingEncodingService;

    private TransportRateLimitService rateLimitService;
    private TransportService transportService;

    @Lazy
    @Autowired
    public void setRateLimitService(TransportRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    public DefaultTransportTenantProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @Override
    public TenantProfile get(@NotNull TenantId tenantId) {
        return getTenantProfile(tenantId);
    }

    @NotNull
    @Override
    public TenantProfileUpdateResult put(@NotNull ByteString profileBody) {
        Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(profileBody.toByteArray());
        if (profileOpt.isPresent()) {
            @NotNull TenantProfile newProfile = profileOpt.get();
            log.trace("[{}] put: {}", newProfile.getId(), newProfile);
            Set<TenantId> affectedTenants = tenantProfileIds.get(newProfile.getId());
            return new TenantProfileUpdateResult(newProfile, affectedTenants != null ? affectedTenants : Collections.emptySet());
        } else {
            log.warn("Failed to decode profile: {}", profileBody);
            return new TenantProfileUpdateResult(null, Collections.emptySet());
        }
    }

    @Override
    public boolean put(TenantId tenantId, TenantProfileId profileId) {
        log.trace("[{}] put: {}", tenantId, profileId);
        TenantProfileId oldProfileId = tenantIds.get(tenantId);
        if (oldProfileId != null && !oldProfileId.equals(profileId)) {
            tenantProfileIds.computeIfAbsent(oldProfileId, id -> ConcurrentHashMap.newKeySet()).remove(tenantId);
            tenantIds.put(tenantId, profileId);
            tenantProfileIds.computeIfAbsent(profileId, id -> ConcurrentHashMap.newKeySet()).add(tenantId);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public Set<TenantId> remove(TenantProfileId profileId) {
        Set<TenantId> tenants = tenantProfileIds.remove(profileId);
        if (tenants != null) {
            tenants.forEach(tenantIds::remove);
        }
        profiles.remove(profileId);
        return tenants;
    }

    @NotNull
    private TenantProfile getTenantProfile(@NotNull TenantId tenantId) {
        @Nullable TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantIds.get(tenantId);
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantIds.get(tenantId);
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                if (profile == null) {
                    TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                            .setEntityType(EntityType.TENANT.name())
                            .setEntityIdMSB(tenantId.getId().getMostSignificantBits())
                            .setEntityIdLSB(tenantId.getId().getLeastSignificantBits())
                            .build();
                    TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                    Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(entityProfileMsg.getData().toByteArray());
                    if (profileOpt.isPresent()) {
                        profile = profileOpt.get();
                        TenantProfile existingProfile = profiles.get(profile.getId());
                        if (existingProfile != null) {
                            profile = existingProfile;
                        } else {
                            profiles.put(profile.getId(), profile);
                        }
                        tenantProfileIds.computeIfAbsent(profile.getId(), id -> ConcurrentHashMap.newKeySet()).add(tenantId);
                        tenantIds.put(tenantId, profile.getId());
                    } else {
                        log.warn("[{}] Can't decode tenant profile: {}", tenantId, entityProfileMsg.getData());
                        throw new RuntimeException("Can't decode tenant profile!");
                    }
                    Optional<ApiUsageState> apiStateOpt = dataDecodingEncodingService.decode(entityProfileMsg.getApiState().toByteArray());
                    apiStateOpt.ifPresent(apiUsageState -> rateLimitService.update(tenantId, apiUsageState.isTransportEnabled()));
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }


}
