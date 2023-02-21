package org.echoiot.server.service.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.RuleEngineTelemetryService;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.BaseAttributeKvEntry;
import org.echoiot.server.common.data.kv.BooleanDataEntry;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.device.ClaimDataInfo;
import org.echoiot.server.dao.device.ClaimDevicesService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.device.claim.ClaimData;
import org.echoiot.server.dao.device.claim.ClaimResponse;
import org.echoiot.server.dao.device.claim.ClaimResult;
import org.echoiot.server.dao.device.claim.ReclaimResult;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@TbCoreComponent
public class ClaimDevicesServiceImpl implements ClaimDevicesService {

    private static final String CLAIM_ATTRIBUTE_NAME = "claimingAllowed";
    private static final String CLAIM_DATA_ATTRIBUTE_NAME = "claimingData";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Resource
    private TbClusterService clusterService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private AttributesService attributesService;
    @Resource
    private RuleEngineTelemetryService telemetryService;
    @Resource
    private CustomerService customerService;
    @Resource
    private CacheManager cacheManager;

    @Value("${security.claim.allowClaimingByDefault}")
    private boolean isAllowedClaimingByDefault;

    @Value("${security.claim.duration}")
    private long systemDurationMs;

    @NotNull
    @Override
    public ListenableFuture<Void> registerClaimingInfo(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs) {
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transformAsync(deviceFuture, device -> {
            @org.jetbrains.annotations.Nullable Cache cache = cacheManager.getCache(CacheConstants.CLAIM_DEVICES_CACHE);
            @NotNull List<Object> key = constructCacheKey(device.getId());

            if (isAllowedClaimingByDefault) {
                if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    persistInCache(secretKey, durationMs, cache, key);
                    return Futures.immediateFuture(null);
                }
                log.warn("The device [{}] has been already claimed!", device.getName());
                throw new IllegalArgumentException();
            } else {
                ListenableFuture<List<AttributeKvEntry>> claimingAllowedFuture = attributesService.find(tenantId, device.getId(),
                                                                                                        DataConstants.SERVER_SCOPE, Collections.singletonList(CLAIM_ATTRIBUTE_NAME));
                return Futures.transform(claimingAllowedFuture, list -> {
                    if (list != null && !list.isEmpty()) {
                        Optional<Boolean> claimingAllowedOptional = list.get(0).getBooleanValue();
                        if (claimingAllowedOptional.isPresent() && claimingAllowedOptional.get()
                                && device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                            persistInCache(secretKey, durationMs, cache, key);
                            return null;
                        }
                    }
                    log.warn("Failed to find claimingAllowed attribute for device or it is already claimed![{}]", device.getName());
                    throw new IllegalArgumentException();
                }, MoreExecutors.directExecutor());
            }
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    private ListenableFuture<ClaimDataInfo> getClaimData(@NotNull Cache cache, @NotNull Device device) {
        @NotNull List<Object> key = constructCacheKey(device.getId());
        @org.jetbrains.annotations.Nullable ClaimData claimDataFromCache = cache.get(key, ClaimData.class);
        if (claimDataFromCache != null) {
            return Futures.immediateFuture(new ClaimDataInfo(true, key, claimDataFromCache));
        } else {
            ListenableFuture<Optional<AttributeKvEntry>> claimDataAttrFuture = attributesService.find(device.getTenantId(), device.getId(),
                    DataConstants.SERVER_SCOPE, CLAIM_DATA_ATTRIBUTE_NAME);

            return Futures.transform(claimDataAttrFuture, claimDataAttr -> {
                if (claimDataAttr.isPresent()) {
                    @org.jetbrains.annotations.Nullable ClaimData claimDataFromAttribute = JacksonUtil.fromString(claimDataAttr.get().getValueAsString(), ClaimData.class);
                    return new ClaimDataInfo(false, key, claimDataFromAttribute);
                }
                return null;
            }, MoreExecutors.directExecutor());
        }
    }

    @NotNull
    @Override
    public ListenableFuture<ClaimResult> claimDevice(@NotNull Device device, CustomerId customerId, @NotNull String secretKey) {
        @org.jetbrains.annotations.Nullable Cache cache = cacheManager.getCache(CacheConstants.CLAIM_DEVICES_CACHE);
        @NotNull ListenableFuture<ClaimDataInfo> claimDataFuture = getClaimData(cache, device);

        return Futures.transformAsync(claimDataFuture, claimData -> {
            if (claimData != null) {
                long currTs = System.currentTimeMillis();
                if (currTs > claimData.getData().getExpirationTime() || !secretKeyIsEmptyOrEqual(secretKey, claimData.getData().getSecretKey())) {
                    log.warn("The claiming timeout occurred or wrong 'secretKey' provided for the device [{}]", device.getName());
                    if (claimData.isFromCache()) {
                        cache.evict(claimData.getKey());
                    }
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
                } else {
                    if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                        device.setCustomerId(customerId);
                        Device savedDevice = deviceService.saveDevice(device);
                        clusterService.onDeviceUpdated(savedDevice, device);
                        return Futures.transform(removeClaimingSavedData(cache, claimData, device), result -> new ClaimResult(savedDevice, ClaimResponse.SUCCESS), MoreExecutors.directExecutor());
                    }
                    return Futures.transform(removeClaimingSavedData(cache, claimData, device), result -> new ClaimResult(null, ClaimResponse.CLAIMED), MoreExecutors.directExecutor());
                }
            } else {
                log.warn("Failed to find the device's claiming message![{}]", device.getName());
                if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
                } else {
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.CLAIMED));
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private boolean secretKeyIsEmptyOrEqual(@NotNull String secretKeyA, String secretKeyB) {
        return (StringUtils.isEmpty(secretKeyA) && StringUtils.isEmpty(secretKeyB)) || secretKeyA.equals(secretKeyB);
    }

    @NotNull
    @Override
    public ListenableFuture<ReclaimResult> reClaimDevice(TenantId tenantId, @NotNull Device device) {
        if (!device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            cacheEviction(device.getId());
            Customer unassignedCustomer = customerService.findCustomerById(tenantId, device.getCustomerId());
            device.setCustomerId(null);
            Device savedDevice = deviceService.saveDevice(device);
            clusterService.onDeviceUpdated(savedDevice, device);
            if (isAllowedClaimingByDefault) {
                return Futures.immediateFuture(new ReclaimResult(unassignedCustomer));
            }
            @NotNull SettableFuture<ReclaimResult> result = SettableFuture.create();
            telemetryService.saveAndNotify(
                    tenantId, savedDevice.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(
                            new BaseAttributeKvEntry(new BooleanDataEntry(CLAIM_ATTRIBUTE_NAME, true), System.currentTimeMillis())
                    ),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            result.set(new ReclaimResult(unassignedCustomer));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            result.setException(t);
                        }
                    });
            return result;
        }
        cacheEviction(device.getId());
        return Futures.immediateFuture(new ReclaimResult(null));
    }

    @NotNull
    private List<Object> constructCacheKey(DeviceId deviceId) {
        return Collections.singletonList(deviceId);
    }

    private void persistInCache(String secretKey, long durationMs, @NotNull Cache cache, @NotNull List<Object> key) {
        @NotNull ClaimData claimData = new ClaimData(secretKey,
                System.currentTimeMillis() + validateDurationMs(durationMs));
        cache.putIfAbsent(key, claimData);
    }

    private long validateDurationMs(long durationMs) {
        if (durationMs > 0L) {
            return durationMs;
        }
        return systemDurationMs;
    }

    @NotNull
    private ListenableFuture<Void> removeClaimingSavedData(@NotNull Cache cache, @NotNull ClaimDataInfo data, @NotNull Device device) {
        if (data.isFromCache()) {
            cache.evict(data.getKey());
        }
        @NotNull SettableFuture<Void> result = SettableFuture.create();
        telemetryService.deleteAndNotify(device.getTenantId(),
                device.getId(), DataConstants.SERVER_SCOPE, Arrays.asList(CLAIM_ATTRIBUTE_NAME, CLAIM_DATA_ATTRIBUTE_NAME), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        result.set(tmp);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        result.setException(t);
                    }
                });
        return result;
    }

    private void cacheEviction(DeviceId deviceId) {
        @org.jetbrains.annotations.Nullable Cache cache = cacheManager.getCache(CacheConstants.CLAIM_DEVICES_CACHE);
        cache.evict(constructCacheKey(deviceId));
    }

}
