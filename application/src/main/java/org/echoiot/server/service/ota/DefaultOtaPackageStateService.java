package org.echoiot.server.service.ota;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleEngineTelemetryService;
import org.echoiot.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.data.ota.OtaPackageKey;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.ota.OtaPackageUpdateStatus;
import org.echoiot.server.common.data.ota.OtaPackageUtil;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.provider.TbCoreQueueFactory;
import org.echoiot.server.queue.provider.TbRuleEngineQueueFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
public class DefaultOtaPackageStateService implements OtaPackageStateService {

    private final TbClusterService tbClusterService;
    private final OtaPackageService otaPackageService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final RuleEngineTelemetryService telemetryService;
    private final TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> otaPackageStateMsgProducer;

    public DefaultOtaPackageStateService(@Lazy TbClusterService tbClusterService,
                                         OtaPackageService otaPackageService,
                                         DeviceService deviceService,
                                         DeviceProfileService deviceProfileService,
                                         @Lazy RuleEngineTelemetryService telemetryService,
                                         Optional<TbCoreQueueFactory> coreQueueFactory,
                                         Optional<TbRuleEngineQueueFactory> reQueueFactory) {
        this.tbClusterService = tbClusterService;
        this.otaPackageService = otaPackageService;
        this.deviceService = deviceService;
        this.deviceProfileService = deviceProfileService;
        this.telemetryService = telemetryService;
        if (coreQueueFactory.isPresent()) {
            this.otaPackageStateMsgProducer = coreQueueFactory.get().createToOtaPackageStateServiceMsgProducer();
        } else {
            this.otaPackageStateMsgProducer = reQueueFactory.get().createToOtaPackageStateServiceMsgProducer();
        }
    }

    @Override
    public void update(Device device, Device oldDevice) {
        updateFirmware(device, oldDevice);
        updateSoftware(device, oldDevice);
    }

    private void updateFirmware(Device device, @org.jetbrains.annotations.Nullable Device oldDevice) {
        OtaPackageId newFirmwareId = device.getFirmwareId();
        if (newFirmwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newFirmwareId = newDeviceProfile.getFirmwareId();
        }
        if (oldDevice != null) {
            OtaPackageId oldFirmwareId = oldDevice.getFirmwareId();
            if (oldFirmwareId == null) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                oldFirmwareId = oldDeviceProfile.getFirmwareId();
            }
            if (newFirmwareId != null) {
                if (!newFirmwareId.equals(oldFirmwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), OtaPackageType.FIRMWARE);
                }
            } else if (oldFirmwareId != null){
                // Device was updated and new firmware is not set.
                remove(device, OtaPackageType.FIRMWARE);
            }
        } else if (newFirmwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), OtaPackageType.FIRMWARE);
        }
    }

    private void updateSoftware(Device device, @org.jetbrains.annotations.Nullable Device oldDevice) {
        OtaPackageId newSoftwareId = device.getSoftwareId();
        if (newSoftwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newSoftwareId = newDeviceProfile.getSoftwareId();
        }
        if (oldDevice != null) {
            OtaPackageId oldSoftwareId = oldDevice.getSoftwareId();
            if (oldSoftwareId == null) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                oldSoftwareId = oldDeviceProfile.getSoftwareId();
            }
            if (newSoftwareId != null) {
                if (!newSoftwareId.equals(oldSoftwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), OtaPackageType.SOFTWARE);
                }
            } else if (oldSoftwareId != null){
                // Device was updated and new firmware is not set.
                remove(device, OtaPackageType.SOFTWARE);
            }
        } else if (newSoftwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), OtaPackageType.SOFTWARE);
        }
    }

    @Override
    public void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged) {
        TenantId tenantId = deviceProfile.getTenantId();

        if (isFirmwareChanged) {
            update(tenantId, deviceProfile, OtaPackageType.FIRMWARE);
        }
        if (isSoftwareChanged) {
            update(tenantId, deviceProfile, OtaPackageType.SOFTWARE);
        }
    }

    private void update(TenantId tenantId, DeviceProfile deviceProfile, OtaPackageType otaPackageType) {
        Consumer<Device> updateConsumer;
        @org.jetbrains.annotations.Nullable OtaPackageId packageId = OtaPackageUtil.getOtaPackageId(deviceProfile, otaPackageType);

        if (packageId != null) {
            long ts = System.currentTimeMillis();
            updateConsumer = d -> send(d.getTenantId(), d.getId(), packageId, ts, otaPackageType);
        } else {
            updateConsumer = d -> remove(d, otaPackageType);
        }

        PageLink pageLink = new PageLink(100);
        PageData<Device> pageData;
        do {
            pageData = deviceService.findDevicesByTenantIdAndTypeAndEmptyOtaPackage(tenantId, deviceProfile.getId(), otaPackageType, pageLink);
            pageData.getData().forEach(updateConsumer);

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public boolean process(ToOtaPackageStateServiceMsg msg) {
        boolean isSuccess = false;
        OtaPackageId targetOtaPackageId = new OtaPackageId(new UUID(msg.getOtaPackageIdMSB(), msg.getOtaPackageIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        OtaPackageType firmwareType = OtaPackageType.valueOf(msg.getType());
        long ts = msg.getTs();

        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("[{}] [{}] Device was removed during firmware update msg was queued!", tenantId, deviceId);
        } else {
            @org.jetbrains.annotations.Nullable OtaPackageId currentOtaPackageId = OtaPackageUtil.getOtaPackageId(device, firmwareType);
            if (currentOtaPackageId == null) {
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, device.getDeviceProfileId());
                currentOtaPackageId = OtaPackageUtil.getOtaPackageId(deviceProfile, firmwareType);
            }

            if (targetOtaPackageId.equals(currentOtaPackageId)) {
                update(device, otaPackageService.findOtaPackageInfoById(device.getTenantId(), targetOtaPackageId), ts);
                isSuccess = true;
            } else {
                log.warn("[{}] [{}] Can`t update firmware for the device, target firmwareId: [{}], current firmwareId: [{}]!", tenantId, deviceId, targetOtaPackageId, currentOtaPackageId);
            }
        }
        return isSuccess;
    }

    private void send(TenantId tenantId, DeviceId deviceId, OtaPackageId firmwareId, long ts, OtaPackageType firmwareType) {
        ToOtaPackageStateServiceMsg msg = ToOtaPackageStateServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setOtaPackageIdMSB(firmwareId.getId().getMostSignificantBits())
                .setOtaPackageIdLSB(firmwareId.getId().getLeastSignificantBits())
                .setType(firmwareType.name())
                .setTs(ts)
                .build();

        OtaPackageInfo firmware = otaPackageService.findOtaPackageInfoById(tenantId, firmwareId);
        if (firmware == null) {
            log.warn("[{}] Failed to send firmware update because firmware was already deleted", firmwareId);
            return;
        }

        TopicPartitionInfo tpi = new TopicPartitionInfo(otaPackageStateMsgProducer.getDefaultTopic(), null, null, false);
        otaPackageStateMsgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);

        List<TsKvEntry> telemetry = new ArrayList<>();
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(OtaPackageUtil.getTargetTelemetryKey(firmware.getType(), OtaPackageKey.TITLE), firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(OtaPackageUtil.getTargetTelemetryKey(firmware.getType(), OtaPackageKey.VERSION), firmware.getVersion())));

        if (StringUtils.isNotEmpty(firmware.getTag())) {
            telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(OtaPackageUtil.getTargetTelemetryKey(firmware.getType(), OtaPackageKey.TAG), firmware.getTag())));
        }

        telemetry.add(new BasicTsKvEntry(ts, new LongDataEntry(OtaPackageUtil.getTargetTelemetryKey(firmware.getType(), OtaPackageKey.TS), ts)));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(OtaPackageUtil.getTelemetryKey(firmware.getType(), OtaPackageKey.STATE), OtaPackageUpdateStatus.QUEUED.name())));

        telemetryService.saveAndNotify(tenantId, deviceId, telemetry, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save firmware status!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save firmware status!", deviceId, t);
            }
        });
    }


    private void update(Device device, OtaPackageInfo otaPackage, long ts) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        OtaPackageType otaPackageType = otaPackage.getType();

        BasicTsKvEntry status = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(OtaPackageUtil.getTelemetryKey(otaPackageType, OtaPackageKey.STATE), OtaPackageUpdateStatus.INITIATED.name()));

        telemetryService.saveAndNotify(tenantId, deviceId, Collections.singletonList(status), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save telemetry with target {} for device!", deviceId, otaPackage);
                updateAttributes(device, otaPackage, ts, tenantId, deviceId, otaPackageType);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save telemetry with target {} for device!", deviceId, otaPackage, t);
                updateAttributes(device, otaPackage, ts, tenantId, deviceId, otaPackageType);
            }
        });
    }

    private void updateAttributes(Device device, OtaPackageInfo otaPackage, long ts, TenantId tenantId, DeviceId deviceId, OtaPackageType otaPackageType) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        List<String> attrToRemove = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.TITLE), otaPackage.getTitle())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.VERSION), otaPackage.getVersion())));
        if (StringUtils.isNotEmpty(otaPackage.getTag())) {
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.TAG), otaPackage.getTag())));
        } else {
            attrToRemove.add(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.TAG));
        }
        if (otaPackage.hasUrl()) {
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.URL), otaPackage.getUrl())));

            if (otaPackage.getDataSize() == null) {
                attrToRemove.add(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.SIZE));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.SIZE), otaPackage.getDataSize())));
            }

            if (otaPackage.getChecksumAlgorithm() != null) {
                attrToRemove.add(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM_ALGORITHM));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM_ALGORITHM), otaPackage.getChecksumAlgorithm().name())));
            }

            if (StringUtils.isEmpty(otaPackage.getChecksum())) {
                attrToRemove.add(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM), otaPackage.getChecksum())));
            }
        } else {
            attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.SIZE), otaPackage.getDataSize())));
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM_ALGORITHM), otaPackage.getChecksumAlgorithm().name())));
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.CHECKSUM), otaPackage.getChecksum())));
            attrToRemove.add(OtaPackageUtil.getAttributeKey(otaPackageType, OtaPackageKey.URL));
        }

        remove(device, otaPackageType, attrToRemove);

        telemetryService.saveAndNotify(tenantId, deviceId, DataConstants.SHARED_SCOPE, attributes, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save attributes with target firmware!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save attributes with target firmware!", deviceId, t);
            }
        });
    }

    private void remove(Device device, OtaPackageType otaPackageType) {
        remove(device, otaPackageType, OtaPackageUtil.getAttributeKeys(otaPackageType));
    }

    private void remove(Device device, OtaPackageType otaPackageType, List<String> attributesKeys) {
        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, attributesKeys,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove target {} attributes!", device.getId(), otaPackageType);
                        tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, attributesKeys), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove target {} attributes!", device.getId(), otaPackageType, t);
                    }
                });
    }
}
