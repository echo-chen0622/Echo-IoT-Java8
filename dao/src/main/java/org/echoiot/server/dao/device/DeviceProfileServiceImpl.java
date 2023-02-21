package org.echoiot.server.dao.device;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceProfileInfo;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.echoiot.server.common.data.DeviceProfileType;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileData;
import org.echoiot.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.queue.QueueService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceProfileServiceImpl extends AbstractCachedEntityService<DeviceProfileCacheKey, DeviceProfile, DeviceProfileEvictEvent> implements DeviceProfileService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    private static final String INCORRECT_DEVICE_PROFILE_NAME = "Incorrect deviceProfileName ";
    private static final String DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Device profile with such name already exists!";

    @Resource
    private DeviceProfileDao deviceProfileDao;

    @Resource
    private DeviceDao deviceDao;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DataValidator<DeviceProfile> deviceProfileValidator;

    @Lazy
    @Resource
    private QueueService queueService;

    @TransactionalEventListener(classes = DeviceProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull DeviceProfileEvictEvent event) {
        @NotNull List<DeviceProfileCacheKey> keys = new ArrayList<>(2);
        keys.add(DeviceProfileCacheKey.fromName(event.getTenantId(), event.getNewName()));
        if (event.getDeviceProfileId() != null) {
            keys.add(DeviceProfileCacheKey.fromId(event.getDeviceProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(DeviceProfileCacheKey.defaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(DeviceProfileCacheKey.fromName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, @NotNull DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.fromId(deviceProfileId),
                () -> deviceProfileDao.findById(tenantId, deviceProfileId.getId()), true);
    }

    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName) {
        log.trace("Executing findDeviceProfileByName [{}][{}]", tenantId, profileName);
        Validator.validateString(profileName, INCORRECT_DEVICE_PROFILE_NAME + profileName);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.fromName(tenantId, profileName),
                () -> deviceProfileDao.findByName(tenantId, profileName), true);
    }

    @Nullable
    @Override
    public DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, @NotNull DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return toDeviceProfileInfo(findDeviceProfileById(tenantId, deviceProfileId));
    }

    @NotNull
    @Override
    public DeviceProfile saveDeviceProfile(@NotNull DeviceProfile deviceProfile) {
        log.trace("Executing saveDeviceProfile [{}]", deviceProfile);
        DeviceProfile oldDeviceProfile = deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
        DeviceProfile savedDeviceProfile;
        try {
            savedDeviceProfile = deviceProfileDao.saveAndFlush(deviceProfile.getTenantId(), deviceProfile);
            publishEvictEvent(new DeviceProfileEvictEvent(savedDeviceProfile.getTenantId(), savedDeviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, savedDeviceProfile.getId(), savedDeviceProfile.isDefault()));
        } catch (Exception t) {
            handleEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, null, deviceProfile.isDefault()));
            checkConstraintViolation(t,
                    Map.of("device_profile_name_unq_key", DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS,
                            "device_provision_key_unq_key", "Device profile with such provision device key already exists!",
                            "device_profile_external_id_unq_key", "Device profile with such external id already exists!"));
            throw t;
        }
        if (oldDeviceProfile != null && !oldDeviceProfile.getName().equals(deviceProfile.getName())) {
            PageLink pageLink = new PageLink(100);
            PageData<Device> pageData;
            do {
                pageData = deviceDao.findDevicesByTenantIdAndProfileId(deviceProfile.getTenantId().getId(), deviceProfile.getUuidId(), pageLink);
                for (@NotNull Device device : pageData.getData()) {
                    device.setType(deviceProfile.getName());
                    deviceService.saveDevice(device);
                }
                pageLink = pageLink.nextPageLink();
            } while (pageData.hasNext());
        }
        return savedDeviceProfile;
    }

    @Override
    @Transactional
    public void deleteDeviceProfile(TenantId tenantId, @NotNull DeviceProfileId deviceProfileId) {
        log.trace("Executing deleteDeviceProfile [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (deviceProfile != null && deviceProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Device Profile is prohibited!");
        }
        this.removeDeviceProfile(tenantId, deviceProfile);
    }

    private void removeDeviceProfile(TenantId tenantId, @NotNull DeviceProfile deviceProfile) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            deleteEntityRelations(tenantId, deviceProfileId);
            deviceProfileDao.removeById(tenantId, deviceProfileId.getId());
            publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    null, deviceProfile.getId(), deviceProfile.isDefault()));
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_device_profile")) {
                throw new DataValidationException("The device profile referenced by the devices cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfileInfos(tenantId, pageLink, transportType);
    }

    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, @NotNull String name) {
        log.trace("Executing findOrCreateDefaultDeviceProfile");
        DeviceProfile deviceProfile = findDeviceProfileByName(tenantId, name);
        if (deviceProfile == null) {
            try {
                deviceProfile = this.doCreateDefaultDeviceProfile(tenantId, name, name.equals("default"));
            } catch (DataValidationException e) {
                if (DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    deviceProfile = findDeviceProfileByName(tenantId, name);
                } else {
                    throw e;
                }
            }
        }
        return deviceProfile;
    }

    @Override
    public DeviceProfile createDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing createDefaultDeviceProfile tenantId [{}]", tenantId);
        return doCreateDefaultDeviceProfile(tenantId, "default", true);
    }

    private DeviceProfile doCreateDefaultDeviceProfile(TenantId tenantId, String profileName, boolean defaultProfile) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setDefault(defaultProfile);
        deviceProfile.setName(profileName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription("Default device profile");
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        @NotNull DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        @NotNull DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        @NotNull DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        return saveDeviceProfile(deviceProfile);
    }

    @Override
    public DeviceProfile findDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.defaultProfile(tenantId),
                () -> deviceProfileDao.findDefaultDeviceProfile(tenantId), true);
    }

    @Nullable
    @Override
    public DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return toDeviceProfileInfo(findDefaultDeviceProfile(tenantId));
    }

    @Override
    public boolean setDefaultDeviceProfile(TenantId tenantId, @NotNull DeviceProfileId deviceProfileId) {
        log.trace("Executing setDefaultDeviceProfile [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (!deviceProfile.isDefault()) {
            deviceProfile.setDefault(true);
            DeviceProfile previousDefaultDeviceProfile = findDefaultDeviceProfile(tenantId);
            boolean changed = false;
            if (previousDefaultDeviceProfile == null) {
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true));
                changed = true;
            } else if (!previousDefaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                previousDefaultDeviceProfile.setDefault(false);
                deviceProfileDao.save(tenantId, previousDefaultDeviceProfile);
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(previousDefaultDeviceProfile.getTenantId(), previousDefaultDeviceProfile.getName(), null, previousDefaultDeviceProfile.getId(), false));
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteDeviceProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDeviceProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDeviceProfilesRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, DeviceProfile> tenantDeviceProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<DeviceProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return deviceProfileDao.findDeviceProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull DeviceProfile entity) {
                    removeDeviceProfile(tenantId, entity);
                }
            };

    private DeviceProfileInfo toDeviceProfileInfo(@Nullable DeviceProfile profile) {
        return profile == null ? null : new DeviceProfileInfo(profile.getId(), profile.getName(), profile.getImage(),
                profile.getDefaultDashboardId(), profile.getType(), profile.getTransportType());
    }

}
