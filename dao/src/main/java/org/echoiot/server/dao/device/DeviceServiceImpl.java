package org.echoiot.server.dao.device;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.cache.device.DeviceCacheEvictEvent;
import org.echoiot.server.cache.device.DeviceCacheKey;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.DeviceSearchQuery;
import org.echoiot.server.common.data.device.credentials.BasicMqttCredentials;
import org.echoiot.server.common.data.device.data.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.dao.device.provision.ProvisionFailedException;
import org.echoiot.server.dao.device.provision.ProvisionRequest;
import org.echoiot.server.dao.device.provision.ProvisionResponseStatus;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.event.EventService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.DaoUtil.toUUIDs;
import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceServiceImpl extends AbstractCachedEntityService<DeviceCacheKey, Device, DeviceCacheEvictEvent> implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    @Resource
    private DeviceDao deviceDao;

    @Resource
    private DeviceCredentialsService deviceCredentialsService;

    @Resource
    private DeviceProfileService deviceProfileService;

    @Resource
    private EventService eventService;

    @Resource
    private DataValidator<Device> deviceValidator;

    @Override
    public DeviceInfo findDeviceInfoById(TenantId tenantId, @NotNull DeviceId deviceId) {
        log.trace("Executing findDeviceInfoById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findDeviceInfoById(tenantId, deviceId.getId());
    }

    @Override
    public Device findDeviceById(TenantId tenantId, @NotNull DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return cache.getAndPutInTransaction(new DeviceCacheKey(tenantId, deviceId),
                () -> {
                    //TODO: possible bug source since sometimes we need to clear cache by tenant id and sometimes by sys tenant id?
                    if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                        return deviceDao.findById(tenantId, deviceId.getId());
                    } else {
                        return deviceDao.findDeviceByTenantIdAndId(tenantId, deviceId.getId());
                    }
                }, true);
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, @NotNull DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return deviceDao.findByIdAsync(tenantId, deviceId.getId());
        } else {
            return deviceDao.findDeviceByTenantIdAndIdAsync(tenantId, deviceId.getId());
        }
    }

    @Override
    public Device findDeviceByTenantIdAndName(@NotNull TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new DeviceCacheKey(tenantId, name),
                () -> deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name).orElse(null), true);
    }

    @Override
    public Device saveDeviceWithAccessToken(@NotNull Device device, String accessToken) {
        return doSaveDevice(device, accessToken, true);
    }

    @Override
    public Device saveDevice(@NotNull Device device, boolean doValidate) {
        return doSaveDevice(device, null, doValidate);
    }

    @Override
    public Device saveDevice(@NotNull Device device) {
        return doSaveDevice(device, null, true);
    }

    @NotNull
    @Transactional
    @Override
    public Device saveDeviceWithCredentials(@NotNull Device device, @NotNull DeviceCredentials deviceCredentials) {
        if (device.getId() == null) {
            Device deviceWithName = this.findDeviceByTenantIdAndName(device.getTenantId(), device.getName());
            device = deviceWithName == null ? device : deviceWithName.updateDevice(device);
        }
        Device savedDevice = this.saveDeviceWithoutCredentials(device, true);
        deviceCredentials.setDeviceId(savedDevice.getId());
        if (device.getId() == null) {
            deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
        } else {
            DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), savedDevice.getId());
            if (foundDeviceCredentials == null) {
                deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
            } else {
                deviceCredentials.setId(foundDeviceCredentials.getId());
                deviceCredentialsService.updateDeviceCredentials(device.getTenantId(), deviceCredentials);
            }
        }
        return savedDevice;
    }

    private Device doSaveDevice(@NotNull Device device, String accessToken, boolean doValidate) {
        Device savedDevice = this.saveDeviceWithoutCredentials(device, doValidate);
        if (device.getId() == null) {
            @NotNull DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(!StringUtils.isEmpty(accessToken) ? accessToken : StringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
        }
        return savedDevice;
    }

    private Device saveDeviceWithoutCredentials(@NotNull Device device, boolean doValidate) {
        log.trace("Executing saveDevice [{}]", device);
        @org.jetbrains.annotations.Nullable Device oldDevice = null;
        if (doValidate) {
            oldDevice = deviceValidator.validate(device, Device::getTenantId);
        } else if (device.getId() != null) {
            oldDevice = findDeviceById(device.getTenantId(), device.getId());
        }
        @NotNull DeviceCacheEvictEvent deviceCacheEvictEvent = new DeviceCacheEvictEvent(device.getTenantId(), device.getId(), device.getName(), oldDevice != null ? oldDevice.getName() : null);
        try {
            DeviceProfile deviceProfile;
            if (device.getDeviceProfileId() == null) {
                if (!StringUtils.isEmpty(device.getType())) {
                    deviceProfile = this.deviceProfileService.findOrCreateDeviceProfile(device.getTenantId(), device.getType());
                } else {
                    deviceProfile = this.deviceProfileService.findDefaultDeviceProfile(device.getTenantId());
                }
                device.setDeviceProfileId(new DeviceProfileId(deviceProfile.getId().getId()));
            } else {
                deviceProfile = this.deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile == null) {
                    throw new DataValidationException("Device is referencing non existing device profile!");
                }
                if (!deviceProfile.getTenantId().equals(device.getTenantId())) {
                    throw new DataValidationException("Device can`t be referencing to device profile from different tenant!");
                }
            }
            device.setType(deviceProfile.getName());
            device.setDeviceData(syncDeviceData(deviceProfile, device.getDeviceData()));
            Device result = deviceDao.saveAndFlush(device.getTenantId(), device);
            publishEvictEvent(deviceCacheEvictEvent);
            return result;
        } catch (Exception t) {
            handleEvictEvent(deviceCacheEvictEvent);
            checkConstraintViolation(t,
                    "device_name_unq_key", "Device with such name already exists!",
                    "device_external_id_unq_key", "Device with such external id already exists!");
            throw t;
        }
    }

    @TransactionalEventListener(classes = DeviceCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull DeviceCacheEvictEvent event) {
        @NotNull List<DeviceCacheKey> keys = new ArrayList<>(3);
        keys.add(new DeviceCacheKey(event.getTenantId(), event.getNewName()));
        if (event.getDeviceId() != null) {
            keys.add(new DeviceCacheKey(event.getTenantId(), event.getDeviceId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new DeviceCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @NotNull
    private DeviceData syncDeviceData(@NotNull DeviceProfile deviceProfile, @org.jetbrains.annotations.Nullable DeviceData deviceData) {
        if (deviceData == null) {
            deviceData = new DeviceData();
        }
        if (deviceData.getConfiguration() == null || !deviceProfile.getType().equals(deviceData.getConfiguration().getType())) {
            if (deviceProfile.getType() == DeviceProfileType.DEFAULT) {
                deviceData.setConfiguration(new DefaultDeviceConfiguration());
            }
        }
        if (deviceData.getTransportConfiguration() == null || !deviceProfile.getTransportType().equals(deviceData.getTransportConfiguration().getType())) {
            switch (deviceProfile.getTransportType()) {
                case DEFAULT:
                    deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
                    break;
                case MQTT:
                    deviceData.setTransportConfiguration(new MqttDeviceTransportConfiguration());
                    break;
                case COAP:
                    deviceData.setTransportConfiguration(new CoapDeviceTransportConfiguration());
                    break;
                case LWM2M:
                    deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
                    break;
                case SNMP:
                    deviceData.setTransportConfiguration(new SnmpDeviceTransportConfiguration());
                    break;
            }
        }
        return deviceData;
    }

    @Transactional
    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, @NotNull DeviceId deviceId, CustomerId customerId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public Device unassignDeviceFromCustomer(TenantId tenantId, @NotNull DeviceId deviceId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public void deleteDevice(final TenantId tenantId, @NotNull final DeviceId deviceId) {
        log.trace("Executing deleteDevice [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        Device device = deviceDao.findById(tenantId, deviceId.getId());
        @NotNull DeviceCacheEvictEvent deviceCacheEvictEvent = new DeviceCacheEvictEvent(device.getTenantId(), device.getId(), device.getName(), null);
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(device.getTenantId(), deviceId);
        if (entityViews != null && !entityViews.isEmpty()) {
            throw new DataValidationException("Can't delete device that has entity views!");
        }

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(tenantId, deviceCredentials);
        }
        deleteEntityRelations(tenantId, deviceId);

        deviceDao.removeById(tenantId, deviceId.getId());

        publishEvictEvent(deviceCacheEvictEvent);
    }


    @Override
    public PageData<Device> findDevicesByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink) {
        log.trace("Executing findTenantDeviceIdPairs, pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceIdInfos(pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(@NotNull TenantId tenantId,
                                                                           @NotNull DeviceProfileId deviceProfileId,
                                                                           OtaPackageType type,
                                                                           PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndTypeAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}], pageLink [{}]",
                tenantId, deviceProfileId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(tenantId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndTypeAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type, pageLink);
    }

    @Override
    public Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(@NotNull TenantId tenantId, @NotNull DeviceProfileId deviceProfileId, OtaPackageType type) {
        log.trace("Executing countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}]", tenantId, deviceProfileId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(tenantId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceDao.countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantIdAndDeviceProfileId(@NotNull TenantId tenantId, @NotNull DeviceProfileId deviceProfileId, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantIdAndDeviceProfileId, tenantId [{}], deviceProfileId [{}], pageLink [{}]", tenantId, deviceProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantIdAndDeviceProfileId(tenantId.getId(), deviceProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public List<Device> findDevicesByIds(@NotNull List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        Validator.validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByIds(toUUIDs(deviceIds));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(@NotNull List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        Validator.validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByIdsAsync(toUUIDs(deviceIds));
    }

    @Transactional
    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDevicesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull DeviceProfileId deviceProfileId, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId, tenantId [{}], customerId [{}], deviceProfileId [{}], pageLink [{}]", tenantId, customerId, deviceProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(tenantId.getId(), customerId.getId(), deviceProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerDeviceUnasigner.removeEntities(tenantId, customerId);
    }

    @NotNull
    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, @NotNull DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        @NotNull ListenableFuture<List<Device>> devices = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            @NotNull List<ListenableFuture<Device>> futures = new ArrayList<>();
            for (@NotNull EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    futures.add(findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        devices = Futures.transform(devices, new Function<>() {
            @Nullable
            @Override
            public List<Device> apply(@Nullable List<Device> deviceList) {
                return deviceList == null ? Collections.emptyList() : deviceList.stream().filter(device -> query.getDeviceTypes().contains(device.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return devices;
    }

    @NotNull
    @Override
    public ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantDeviceTypes = deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
        return Futures.transform(tenantDeviceTypes,
                deviceTypes -> {
                    deviceTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return deviceTypes;
                }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Transactional
    @Override
    public Device assignDeviceToTenant(TenantId tenantId, @NotNull Device device) {
        log.trace("Executing assignDeviceToTenant [{}][{}]", tenantId, device);
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(device.getTenantId(), device.getId());
        if (!CollectionUtils.isEmpty(entityViews)) {
            throw new DataValidationException("Can't assign device that has entity views to another tenant!");
        }

        eventService.removeEvents(device.getTenantId(), device.getId());

        relationService.removeRelations(device.getTenantId(), device.getId());

        TenantId oldTenantId = device.getTenantId();

        device.setTenantId(tenantId);
        device.setCustomerId(null);
        device.setDeviceProfileId(null);
        Device savedDevice = doSaveDevice(device, null, true);

        @NotNull DeviceCacheEvictEvent oldTenantEvent = new DeviceCacheEvictEvent(oldTenantId, device.getId(), device.getName(), null);
        @NotNull DeviceCacheEvictEvent newTenantEvent = new DeviceCacheEvictEvent(savedDevice.getTenantId(), device.getId(), device.getName(), null);

        // explicitly remove device with previous tenant id from cache
        // result device object will have different tenant id and will not remove entity from cache
        publishEvictEvent(oldTenantEvent);
        publishEvictEvent(newTenantEvent);

        return savedDevice;
    }

    @NotNull
    @Override
    @Transactional
    public Device saveDevice(@NotNull ProvisionRequest provisionRequest, @NotNull DeviceProfile profile) {
        @NotNull Device device = new Device();
        device.setName(provisionRequest.getDeviceName());
        device.setType(profile.getName());
        device.setTenantId(profile.getTenantId());
        Device savedDevice = saveDevice(device);
        if (!StringUtils.isEmpty(provisionRequest.getCredentialsData().getToken()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getX509CertHash()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getUsername()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getPassword()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getClientId())) {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedDevice.getTenantId(), savedDevice.getId());
            if (deviceCredentials == null) {
                deviceCredentials = new DeviceCredentials();
            }
            deviceCredentials.setDeviceId(savedDevice.getId());
            deviceCredentials.setCredentialsType(provisionRequest.getCredentialsType());
            switch (provisionRequest.getCredentialsType()) {
                case ACCESS_TOKEN:
                    deviceCredentials.setCredentialsId(provisionRequest.getCredentialsData().getToken());
                    break;
                case MQTT_BASIC:
                    @NotNull BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
                    mqttCredentials.setClientId(provisionRequest.getCredentialsData().getClientId());
                    mqttCredentials.setUserName(provisionRequest.getCredentialsData().getUsername());
                    mqttCredentials.setPassword(provisionRequest.getCredentialsData().getPassword());
                    deviceCredentials.setCredentialsValue(JacksonUtil.toString(mqttCredentials));
                    break;
                case X509_CERTIFICATE:
                    deviceCredentials.setCredentialsValue(provisionRequest.getCredentialsData().getX509CertHash());
                    break;
                case LWM2M_CREDENTIALS:
                    break;
            }
            try {
                deviceCredentialsService.updateDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
            } catch (Exception e) {
                throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
            }
        }

        publishEvictEvent(new DeviceCacheEvictEvent(savedDevice.getTenantId(), savedDevice.getId(), provisionRequest.getDeviceName(), null));
        return savedDevice;
    }

    @Override
    public PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink) {
        return deviceDao.findDevicesIdsByDeviceProfileTransportType(transportType, pageLink);
    }

    @NotNull
    @Override
    public Device assignDeviceToEdge(TenantId tenantId, @NotNull DeviceId deviceId, EdgeId edgeId) {
        Device device = findDeviceById(tenantId, deviceId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign device to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(device.getTenantId().getId())) {
            throw new DataValidationException("Can't assign device to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create device relation. Edge Id: [{}]", deviceId, edgeId);
            throw new RuntimeException(e);
        }
        return device;
    }

    @Override
    public Device unassignDeviceFromEdge(TenantId tenantId, @NotNull DeviceId deviceId, EdgeId edgeId) {
        Device device = findDeviceById(tenantId, deviceId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign device from non-existent edge!");
        }

        checkAssignedEntityViewsToEdge(tenantId, deviceId, edgeId);

        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete device relation. Edge Id: [{}]", deviceId, edgeId);
            throw new RuntimeException(e);
        }
        return device;
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeId(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return deviceDao.countByTenantId(tenantId);
    }

    private final PaginatedRemover<TenantId, Device> tenantDevicesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Device> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull Device entity) {
                    deleteDevice(tenantId, new DeviceId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<CustomerId, Device> customerDeviceUnasigner = new PaginatedRemover<CustomerId, Device>() {

        @Override
        protected PageData<Device> findEntities(@NotNull TenantId tenantId, @NotNull CustomerId id, PageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull Device entity) {
            unassignDeviceFromCustomer(tenantId, new DeviceId(entity.getUuidId()));
        }
    };
}
