package org.echoiot.server.dao.ota;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cache.ota.OtaPackageDataCache;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.ByteBuffer;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseOtaPackageService extends AbstractCachedEntityService<OtaPackageCacheKey, OtaPackageInfo, OtaPackageCacheEvictEvent> implements OtaPackageService {
    public static final String INCORRECT_OTA_PACKAGE_ID = "Incorrect otaPackageId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @NotNull
    private final OtaPackageDao otaPackageDao;
    @NotNull
    private final OtaPackageInfoDao otaPackageInfoDao;
    @NotNull
    private final OtaPackageDataCache otaPackageDataCache;
    @NotNull
    private final DataValidator<OtaPackageInfo> otaPackageInfoValidator;
    @NotNull
    private final DataValidator<OtaPackage> otaPackageValidator;

    @TransactionalEventListener(classes = OtaPackageCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull OtaPackageCacheEvictEvent event) {
        cache.evict(new OtaPackageCacheKey(event.getId()));
        otaPackageDataCache.evict(event.getId().toString());
    }

    @Override
    public OtaPackageInfo saveOtaPackageInfo(@NotNull OtaPackageInfo otaPackageInfo, boolean isUrl) {
        log.trace("Executing saveOtaPackageInfo [{}]", otaPackageInfo);
        if (isUrl && (StringUtils.isEmpty(otaPackageInfo.getUrl()) || otaPackageInfo.getUrl().trim().length() == 0)) {
            throw new DataValidationException("Ota package URL should be specified!");
        }
        otaPackageInfoValidator.validate(otaPackageInfo, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            var result = otaPackageInfoDao.save(otaPackageInfo.getTenantId(), otaPackageInfo);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public OtaPackage saveOtaPackage(@NotNull OtaPackage otaPackage) {
        log.trace("Executing saveOtaPackage [{}]", otaPackage);
        otaPackageValidator.validate(otaPackage, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackage.getId();
        try {
            var result = otaPackageDao.save(otaPackage.getTenantId(), otaPackage);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @NotNull
    @Override
    public String generateChecksum(@NotNull ChecksumAlgorithm checksumAlgorithm, @NotNull ByteBuffer data) {
        if (data == null || !data.hasArray() || data.array().length == 0) {
            throw new DataValidationException("OtaPackage data should be specified!");
        }

        return getHashFunction(checksumAlgorithm).hashBytes(data.array()).toString();
    }

    @NotNull
    @SuppressWarnings("deprecation")
    private HashFunction getHashFunction(@NotNull ChecksumAlgorithm checksumAlgorithm) {
        switch (checksumAlgorithm) {
            case MD5:
                return Hashing.md5();
            case SHA256:
                return Hashing.sha256();
            case SHA384:
                return Hashing.sha384();
            case SHA512:
                return Hashing.sha512();
            case CRC32:
                return Hashing.crc32();
            case MURMUR3_32:
                return Hashing.murmur3_32();
            case MURMUR3_128:
                return Hashing.murmur3_128();
            default:
                throw new DataValidationException("Unknown checksum algorithm!");
        }
    }

    @Override
    public OtaPackage findOtaPackageById(TenantId tenantId, @NotNull OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageDao.findById(tenantId, otaPackageId.getId());
    }

    @Override
    public OtaPackageInfo findOtaPackageInfoById(TenantId tenantId, @NotNull OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return cache.getAndPutInTransaction(new OtaPackageCacheKey(otaPackageId),
                () -> otaPackageInfoDao.findById(tenantId, otaPackageId.getId()), true);
    }

    @Override
    public ListenableFuture<OtaPackageInfo> findOtaPackageInfoByIdAsync(TenantId tenantId, @NotNull OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoByIdAsync [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageInfoDao.findByIdAsync(tenantId, otaPackageId.getId());
    }

    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantIdAndHasData, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, otaPackageType, pageLink);
    }

    @Override
    public void deleteOtaPackage(TenantId tenantId, @NotNull OtaPackageId otaPackageId) {
        log.trace("Executing deleteOtaPackage [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        try {
            otaPackageDao.removeById(tenantId, otaPackageId.getId());
            publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device")) {
                throw new DataValidationException("The otaPackage referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device_profile")) {
                throw new DataValidationException("The otaPackage referenced by the device profile cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device")) {
                throw new DataValidationException("The software referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device_profile")) {
                throw new DataValidationException("The software referenced by the device profile cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageDao.sumDataSizeByTenantId(tenantId);
    }

    @Override
    public void deleteOtaPackagesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteOtaPackagesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantOtaPackageRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, OtaPackageInfo> tenantOtaPackageRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<OtaPackageInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return otaPackageInfoDao.findOtaPackageInfoByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull OtaPackageInfo entity) {
                    deleteOtaPackage(tenantId, entity.getId());
                }
            };

}
