package org.echoiot.server.transport.lwm2m.server.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.DonAsynchron;
import org.echoiot.server.cache.ota.OtaPackageDataCache;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.echoiot.server.common.data.ota.OtaPackageKey;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.ota.OtaPackageUpdateStatus;
import org.echoiot.server.common.data.ota.OtaPackageUtil;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.echoiot.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.echoiot.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.echoiot.server.transport.lwm2m.server.downlink.*;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.ota.firmware.*;
import org.echoiot.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;
import org.echoiot.server.transport.lwm2m.server.ota.software.LwM2MSoftwareUpdateStrategy;
import org.echoiot.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.echoiot.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;
import org.echoiot.server.transport.lwm2m.server.store.TbLwM2MClientOtaInfoStore;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;
import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MOtaUpdateService extends LwM2MExecutorAwareService implements LwM2MOtaUpdateService {

    public static final String FIRMWARE_VERSION = OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION);
    public static final String FIRMWARE_TITLE = OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE);
    public static final String FIRMWARE_TAG = OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TAG);
    public static final String FIRMWARE_URL = OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.URL);
    public static final String SOFTWARE_VERSION = OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION);
    public static final String SOFTWARE_TITLE = OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE);
    public static final String SOFTWARE_TAG = OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TAG);
    public static final String SOFTWARE_URL = OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.URL);

    public static final String FIRMWARE_UPDATE_COAP_RESOURCE = "tbfw";
    public static final String SOFTWARE_UPDATE_COAP_RESOURCE = "tbsw";
    private static final String FW_PACKAGE_5_ID = "/5/0/0";
    private static final String FW_PACKAGE_19_ID = "/19/0/0";
    private static final String FW_URL_ID = "/5/0/1";
    private static final String FW_EXECUTE_ID = "/5/0/2";
    public static final String FW_STATE_ID = "/5/0/3";
    public static final String FW_RESULT_ID = "/5/0/5";
    public static final String FW_NAME_ID = "/5/0/6";
    public static final String FW_VER_ID = "/5/0/7";
    /**
     * Quectel@Hi15RM1-HLB_V1.0@BC68JAR01A10,V150R100C20B300SP7,V150R100C20B300SP7@8
     * Revision:BC68JAR01A10
     */
    public static final String FW_3_VER_ID = "/3/0/3";
    public static final String FW_DELIVERY_METHOD = "/5/0/9";

    public static final String SW_3_VER_ID = "/3/0/19";

    public static final String SW_NAME_ID = "/9/0/0";
    public static final String SW_VER_ID = "/9/0/1";
    public static final String SW_PACKAGE_ID = "/9/0/2";
    public static final String SW_PACKAGE_URI_ID = "/9/0/3";
    public static final String SW_INSTALL_ID = "/9/0/4";
    public static final String SW_STATE_ID = "/9/0/7";
    public static final String SW_RESULT_ID = "/9/0/9";
    public static final String SW_UN_INSTALL_ID = "/9/0/6";

    private final Map<String, LwM2MClientFwOtaInfo> fwStates = new ConcurrentHashMap<>();
    private final Map<String, LwM2MClientSwOtaInfo> swStates = new ConcurrentHashMap<>();

    @NotNull
    private final TransportService transportService;
    @NotNull
    private final LwM2mClientContext clientContext;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2mUplinkMsgHandler uplinkHandler;
    @NotNull
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    @NotNull
    private final OtaPackageDataCache otaPackageDataCache;
    @NotNull
    private final LwM2MTelemetryLogService logService;
    @NotNull
    private final LwM2mTransportServerHelper helper;
    @NotNull
    private final TbLwM2MClientOtaInfoStore otaInfoStore;

    @Resource
    @Lazy
    private LwM2MAttributesService attributesService;

    @PostConstruct
    public void init() {
        super.init();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected int getExecutorSize() {
        return config.getOtaPoolSize();
    }

    @NotNull
    @Override
    protected String getExecutorName() {
        return "LwM2M OTA";
    }

    @Override
    public void init(@NotNull LwM2mClient client) {
        //TODO: add locks by client fwInfo.
        //TODO: check that the client supports FW and SW by checking the supported objects in the model.
        @NotNull List<String> attributesToFetch = new ArrayList<>();
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);

        if (fwInfo.isSupported()) {
            attributesToFetch.add(FIRMWARE_TITLE);
            attributesToFetch.add(FIRMWARE_VERSION);
            attributesToFetch.add(FIRMWARE_TAG);
            attributesToFetch.add(FIRMWARE_URL);
        }

        @NotNull LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        if (swInfo.isSupported()) {
            attributesToFetch.add(SOFTWARE_TITLE);
            attributesToFetch.add(SOFTWARE_VERSION);
            attributesToFetch.add(SOFTWARE_TAG);
            attributesToFetch.add(SOFTWARE_URL);
        }

        var clientSettings = clientContext.getProfile(client.getProfileId()).getClientLwM2mSettings();
        initFwStrategy(client, clientSettings);
        initSwStrategy(client, clientSettings);

        if (!attributesToFetch.isEmpty()) {
            var future = attributesService.getSharedAttributes(client, attributesToFetch);
            DonAsynchron.withCallback(future, attrs -> {
                if (fwInfo.isSupported()) {
                    @NotNull Optional<String> newFwTitle = getAttributeValue(attrs, FIRMWARE_TITLE);
                    @NotNull Optional<String> newFwVersion = getAttributeValue(attrs, FIRMWARE_VERSION);
                    @NotNull Optional<String> newFwTag = getAttributeValue(attrs, FIRMWARE_TAG);
                    @NotNull Optional<String> newFwUrl = getAttributeValue(attrs, FIRMWARE_URL);
                    if (newFwTitle.isPresent() && newFwVersion.isPresent() && !isOtaDownloading(client) && !OtaPackageUpdateStatus.UPDATING.equals(fwInfo.status)) {
                        onTargetFirmwareUpdate(client, newFwTitle.get(), newFwVersion.get(), newFwUrl, newFwTag);
                    }
                }
                if (swInfo.isSupported()) {
                    @NotNull Optional<String> newSwTitle = getAttributeValue(attrs, SOFTWARE_TITLE);
                    @NotNull Optional<String> newSwVersion = getAttributeValue(attrs, SOFTWARE_VERSION);
                    @NotNull Optional<String> newSwTag = getAttributeValue(attrs, SOFTWARE_TAG);
                    @NotNull Optional<String> newSwUrl = getAttributeValue(attrs, SOFTWARE_URL);
                    if (newSwTitle.isPresent() && newSwVersion.isPresent()) {
                        onTargetSoftwareUpdate(client, newSwTitle.get(), newSwVersion.get(), newSwUrl, newSwTag);
                    }
                }
            }, throwable -> {
                if (fwInfo.isSupported()) {
                    update(fwInfo);
                }
            }, executor);
        }
    }

    @Override
    public void forceFirmwareUpdate(@NotNull LwM2mClient client) {
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setRetryAttempts(0);
        fwInfo.setFailedPackageId(null);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onTargetFirmwareUpdate(@NotNull LwM2mClient client, String newFirmwareTitle, String newFirmwareVersion, @NotNull Optional<String> newFirmwareUrl, @NotNull Optional<String> newFirmwareTag) {
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.updateTarget(newFirmwareTitle, newFirmwareVersion, newFirmwareUrl, newFirmwareTag);
        update(fwInfo);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onCurrentFirmwareNameUpdate(@NotNull LwM2mClient client, String name) {
        log.debug("[{}] Current fw name: {}", client.getEndpoint(), name);
        getOrInitFwInfo(client).setCurrentName(name);
    }

    @Override
    public void onCurrentSoftwareNameUpdate(@NotNull LwM2mClient client, String name) {
        log.debug("[{}] Current sw name: {}", client.getEndpoint(), name);
        getOrInitSwInfo(client).setCurrentName(name);
    }

    @Override
    public void onFirmwareStrategyUpdate(@NotNull LwM2mClient client, @NotNull OtherConfiguration configuration) {
        log.debug("[{}] Current fw strategy: {}", client.getEndpoint(), configuration.getFwUpdateStrategy());
        startFirmwareUpdateIfNeeded(client, initFwStrategy(client, configuration));
    }

    @NotNull
    private LwM2MClientFwOtaInfo initFwStrategy(@NotNull LwM2mClient client, @NotNull OtherConfiguration configuration) {
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setStrategy(LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(configuration.getFwUpdateStrategy()));
        fwInfo.setBaseUrl(configuration.getFwUpdateResource());
        return fwInfo;
    }

    @Override
    public void onCurrentSoftwareStrategyUpdate(@NotNull LwM2mClient client, @NotNull OtherConfiguration configuration) {
        log.debug("[{}] Current sw strategy: {}", client.getEndpoint(), configuration.getSwUpdateStrategy());
        startSoftwareUpdateIfNeeded(client, initSwStrategy(client, configuration));
    }

    @NotNull
    private LwM2MClientSwOtaInfo initSwStrategy(@NotNull LwM2mClient client, @NotNull OtherConfiguration configuration) {
        @NotNull LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        swInfo.setStrategy(LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(configuration.getSwUpdateStrategy()));
        swInfo.setBaseUrl(configuration.getSwUpdateResource());
        return swInfo;
    }

    @Override
    public void onCurrentFirmwareVersion3Update(@NotNull LwM2mClient client, String version) {
        log.debug("[{}] Current fw version(3): {}", client.getEndpoint(), version);
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion3(version);
    }

    @Override
    public void onCurrentFirmwareVersionUpdate(@NotNull LwM2mClient client, String version) {
        log.debug("[{}] Current fw version(5): {}", client.getEndpoint(), version);
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion(version);
    }

    @Override
    public void onCurrentFirmwareStateUpdate(@NotNull LwM2mClient client, @NotNull Long stateCode) {
        log.debug("[{}] Current fw state: {}", client.getEndpoint(), stateCode);
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        @NotNull FirmwareUpdateState state = FirmwareUpdateState.fromStateFwByCode(stateCode.intValue());
        if (FirmwareUpdateState.DOWNLOADED.equals(state)) {
            executeFwUpdate(client);
        }
        fwInfo.setUpdateState(state);
        @NotNull Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);

        if (FirmwareUpdateState.IDLE.equals(state) && OtaPackageUpdateStatus.DOWNLOADING.equals(fwInfo.getStatus())) {
            fwInfo.setFailedPackageId(fwInfo.getTargetPackageId());
            status = Optional.of(OtaPackageUpdateStatus.FAILED);
        }

        status.ifPresent(otaStatus -> {
            fwInfo.setStatus(otaStatus);
            sendStateUpdateToTelemetry(client, fwInfo,
                    otaStatus, "Firmware Update State: " + state.name());
        });
        update(fwInfo);
    }

    @Override
    public void onCurrentFirmwareResultUpdate(@NotNull LwM2mClient client, @NotNull Long code) {
        log.debug("[{}] Current fw result: {}", client.getEndpoint(), code);
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        @NotNull FirmwareUpdateResult result = FirmwareUpdateResult.fromUpdateResultFwByCode(code.intValue());
        @NotNull Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);

        if (FirmwareUpdateResult.INITIAL.equals(result) && OtaPackageUpdateStatus.UPDATING.equals(fwInfo.getStatus())) {
            status = Optional.of(OtaPackageUpdateStatus.UPDATED);
            fwInfo.setRetryAttempts(0);
            fwInfo.setFailedPackageId(null);
        }

        status.ifPresent(otaStatus -> {
                    fwInfo.setStatus(otaStatus);
                    sendStateUpdateToTelemetry(client, fwInfo,
                            otaStatus, "Firmware Update Result: " + result.name());
                }
        );

        if (result.isAgain() && fwInfo.getRetryAttempts() <= 2) {
            fwInfo.setRetryAttempts(fwInfo.getRetryAttempts() + 1);
            startFirmwareUpdateIfNeeded(client, fwInfo);
        } else {
            fwInfo.update(result);
        }
        update(fwInfo);
    }

    @Override
    public void onCurrentFirmwareDeliveryMethodUpdate(@NotNull LwM2mClient client, @NotNull Long value) {
        log.debug("[{}] Current fw delivery method: {}", client.getEndpoint(), value);
        @NotNull LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setDeliveryMethod(value.intValue());
    }

    @Override
    public void onCurrentSoftwareVersion3Update(@NotNull LwM2mClient client, String version) {
        log.debug("[{}] Current sw version(3): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion3(version);
    }

    @Override
    public void onCurrentSoftwareVersionUpdate(@NotNull LwM2mClient client, String version) {
        log.debug("[{}] Current sw version(9): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion(version);
    }

    @Override
    public void onCurrentSoftwareStateUpdate(@NotNull LwM2mClient client, @NotNull Long stateCode) {
        log.debug("[{}] Current sw state: {}", client.getEndpoint(), stateCode);
        @NotNull LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        @NotNull SoftwareUpdateState state = SoftwareUpdateState.fromUpdateStateSwByCode(stateCode.intValue());
        if (SoftwareUpdateState.INITIAL.equals(state)) {
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else if (SoftwareUpdateState.DELIVERED.equals(state)) {
            executeSwInstall(client);
        }
        swInfo.setUpdateState(state);
        @NotNull Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Firmware Update State: " + state.name()));
        update(swInfo);
    }


    @Override
    public void onCurrentSoftwareResultUpdate(@NotNull LwM2mClient client, @NotNull Long code) {
        log.debug("[{}] Current sw result: {}", client.getEndpoint(), code);
        @NotNull LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        @NotNull SoftwareUpdateResult result = SoftwareUpdateResult.fromUpdateResultSwByCode(code.intValue());
        @NotNull Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Software Update Result: " + result.name()));
        if (result.isAgain() && swInfo.getRetryAttempts() <= 2) {
            swInfo.setRetryAttempts(swInfo.getRetryAttempts() + 1);
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else {
            swInfo.update(result);
        }
        update(swInfo);
    }

    @Override
    public void onTargetSoftwareUpdate(@NotNull LwM2mClient client, String newSoftwareTitle, String newSoftwareVersion, @NotNull Optional<String> newSoftwareUrl, @NotNull Optional<String> newSoftwareTag) {
        @NotNull LwM2MClientSwOtaInfo fwInfo = getOrInitSwInfo(client);
        fwInfo.updateTarget(newSoftwareTitle, newSoftwareVersion, newSoftwareUrl, newSoftwareTag);
        update(fwInfo);
        startSoftwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public boolean isOtaDownloading(@NotNull LwM2mClient client) {
        String endpoint = client.getEndpoint();
        LwM2MClientFwOtaInfo fwInfo = fwStates.get(endpoint);
        LwM2MClientSwOtaInfo swInfo = swStates.get(endpoint);

        if (fwInfo != null && (OtaPackageUpdateStatus.DOWNLOADING.equals(fwInfo.getStatus()))) {
            return true;
        }
        return swInfo != null && (OtaPackageUpdateStatus.DOWNLOADING.equals(swInfo.getStatus()));
    }

    private void startFirmwareUpdateIfNeeded(@NotNull LwM2mClient client, @NotNull LwM2MClientFwOtaInfo fwInfo) {
        try {
            if (!fwInfo.isSupported() && fwInfo.isAssigned()) {
                log.debug("[{}] Fw update is not supported: {}", client.getEndpoint(), fwInfo);
                sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Client does not support firmware update or profile misconfiguration!");
            } else if (fwInfo.isUpdateRequired()) {
                if (StringUtils.isNotEmpty(fwInfo.getTargetUrl())) {
                    log.debug("[{}] Starting update to [{}{}][] using URL: {}", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion(), fwInfo.getTargetUrl());
                    startUpdateUsingUrl(client, FW_URL_ID, fwInfo.getTargetUrl());
                } else {
                    log.debug("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion());
                    startUpdateUsingBinary(client, fwInfo);
                }
            } else if (fwInfo.getResult() != null && fwInfo.getResult().getCode() > FirmwareUpdateResult.UPDATE_SUCCESSFULLY.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), fwInfo);
                logService.log(client, "Previous update firmware failed. Result: " + fwInfo.getResult().name());
            }
        } catch (Exception e) {
            log.error("[{}] failed to update client: {}", client.getEndpoint(), fwInfo, e);
            sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    private void startSoftwareUpdateIfNeeded(@NotNull LwM2mClient client, @NotNull LwM2MClientSwOtaInfo swInfo) {
        try {
            if (!swInfo.isSupported() && swInfo.isAssigned()) {
                log.debug("[{}] Sw update is not supported: {}", client.getEndpoint(), swInfo);
                sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Client does not support software update or profile misconfiguration!");
            } else if (swInfo.isUpdateRequired()) {
                if (SoftwareUpdateState.INSTALLED.equals(swInfo.getUpdateState())) {
                    log.debug("[{}] Attempt to restore the update state: {}", client.getEndpoint(), swInfo.getUpdateState());
                    executeSwUninstallForUpdate(client);
                } else {
                    if (StringUtils.isNotEmpty(swInfo.getTargetUrl())) {
                        log.debug("[{}] Starting update to [{}{}] using URL: {}", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion(), swInfo.getTargetUrl());
                        startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, swInfo.getTargetUrl());
                    } else {
                        log.debug("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion());
                        startUpdateUsingBinary(client, swInfo);
                    }
                }
            } else if (swInfo.getResult() != null && swInfo.getResult().getCode() >= SoftwareUpdateResult.NOT_ENOUGH_STORAGE.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), swInfo);
                logService.log(client, "Previous update software failed. Result: " + swInfo.getResult().name());
            }
        } catch (Exception e) {
            log.info("[{}] failed to update client: {}", client.getEndpoint(), swInfo, e);
            sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    public void startUpdateUsingBinary(@NotNull LwM2mClient client, @NotNull LwM2MClientSwOtaInfo swInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), swInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(@NotNull TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateSoftwareUsingBinary(response, swInfo, client));
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        logService.log(client, "Failed to process software update: " + e.getMessage());
                    }
                });
    }

    private void startUpdateUsingUrl(@NotNull LwM2mClient client, @NotNull String id, String url) {
        @Nullable String targetIdVer = convertObjectIdToVersionedId(id, client.getRegistration());
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(targetIdVer).value(url).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, targetIdVer));
    }

    public void startUpdateUsingBinary(@NotNull LwM2mClient client, @NotNull LwM2MClientFwOtaInfo fwInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), fwInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(@NotNull TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateFirmwareUsingBinary(response, fwInfo, client));
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        logService.log(client, "Failed to process firmware update: " + e.getMessage());
                    }
                });
    }

    private void doUpdateFirmwareUsingBinary(@NotNull TransportProtos.GetOtaPackageResponseMsg response, @NotNull LwM2MClientFwOtaInfo info, @NotNull LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            @NotNull UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MFirmwareUpdateStrategy strategy;
            if (info.getDeliveryMethod() == null || info.getDeliveryMethod() == FirmwareDeliveryMethod.BOTH.code) {
                strategy = info.getStrategy();
            } else {
                strategy = info.getDeliveryMethod() == FirmwareDeliveryMethod.PULL.code ? LwM2MFirmwareUpdateStrategy.OBJ_5_TEMP_URL : LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY;
            }
            switch (strategy) {
                case OBJ_5_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_5_ID, client.getRegistration()), otaPackageId);
                    break;
                case OBJ_19_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_19_ID, client.getRegistration()), otaPackageId);
                    break;
                case OBJ_5_TEMP_URL:
                    startUpdateUsingUrl(client, FW_URL_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId);
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    private void doUpdateSoftwareUsingBinary(@NotNull TransportProtos.GetOtaPackageResponseMsg response, @NotNull LwM2MClientSwOtaInfo info, @NotNull LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            @NotNull UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MSoftwareUpdateStrategy strategy = info.getStrategy();
            switch (strategy) {
                case BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(SW_PACKAGE_ID, client.getRegistration()), otaPackageId);
                    break;
                case TEMP_URL:
                    startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId);
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    private void startUpdateUsingBinary(LwM2mClient client, String versionedId, @NotNull UUID otaPackageId) {
        byte[] firmwareChunk = otaPackageDataCache.get(otaPackageId.toString(), 0, 0);
        TbLwM2MWriteReplaceRequest writeRequest = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                .value(firmwareChunk).contentFormat(ContentFormat.OPAQUE)
                .timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, writeRequest, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId));
    }

    private TransportProtos.GetOtaPackageRequestMsg createOtaPackageRequestMsg(@NotNull TransportProtos.SessionInfoProto sessionInfo, String nameFwSW) {
        return TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                .setType(nameFwSW)
                .build();
    }

    private void executeFwUpdate(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(FW_EXECUTE_ID).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, FW_EXECUTE_ID));
    }

    private void executeSwInstall(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(SW_INSTALL_ID).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, SW_INSTALL_ID));
    }

    private void executeSwUninstallForUpdate(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(SW_UN_INSTALL_ID).params("1").timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, SW_INSTALL_ID));
    }

    @NotNull
    private Optional<String> getAttributeValue(@NotNull List<TransportProtos.TsKvProto> attrs, @NotNull String keyName) {
        for (@NotNull TransportProtos.TsKvProto attr : attrs) {
            if (keyName.equals(attr.getKv().getKey())) {
                if (attr.getKv().getType().equals(TransportProtos.KeyValueType.STRING_V)) {
                    return Optional.of(attr.getKv().getStringV());
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    private LwM2MClientFwOtaInfo getOrInitFwInfo(@NotNull LwM2mClient client) {
        return this.fwStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientFwOtaInfo info = otaInfoStore.getFw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getProfileId());
                info = new LwM2MClientFwOtaInfo(endpoint, profile.getClientLwM2mSettings().getFwUpdateResource(),
                        LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(profile.getClientLwM2mSettings().getFwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    @NotNull
    private LwM2MClientSwOtaInfo getOrInitSwInfo(@NotNull LwM2mClient client) {
        return this.swStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientSwOtaInfo info = otaInfoStore.getSw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getProfileId());
                info = new LwM2MClientSwOtaInfo(endpoint, profile.getClientLwM2mSettings().getSwUpdateResource(),
                        LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(profile.getClientLwM2mSettings().getSwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    private void update(LwM2MClientFwOtaInfo info) {
        otaInfoStore.putFw(info);
    }

    private void update(LwM2MClientSwOtaInfo info) {
        otaInfoStore.putSw(info);
    }

    private void sendStateUpdateToTelemetry(@NotNull LwM2mClient client, @NotNull LwM2MClientOtaInfo<?, ?, ?> fwInfo, @NotNull OtaPackageUpdateStatus status, String log) {
        @NotNull List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(OtaPackageUtil.getAttributeKey(fwInfo.getType(), OtaPackageKey.STATE));
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(status.name());
        result.add(kvProto.build());
        kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY);
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(log);
        result.add(kvProto.build());
        helper.sendParametersOnEchoiotTelemetry(result, client.getSession(), client.getKeyTsLatestMap());
    }

    @NotNull
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(@NotNull FirmwareUpdateResult fwUpdateResult) {
        switch (fwUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case UPDATE_SUCCESSFULLY:
                return Optional.of(OtaPackageUpdateStatus.UPDATED);
            case NOT_ENOUGH:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case INTEGRITY_CHECK_FAILURE:
            case UNSUPPORTED_TYPE:
            case INVALID_URI:
            case UPDATE_FAILED:
            case UNSUPPORTED_PROTOCOL:
                return Optional.of(OtaPackageUpdateStatus.FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", fwUpdateResult.name());
        }
    }

    @NotNull
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(@NotNull FirmwareUpdateState firmwareUpdateState) {
        switch (firmwareUpdateState) {
            case IDLE:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADED);
            case UPDATING:
                return Optional.of(OtaPackageUpdateStatus.UPDATING);
            default:
                throw new CodecException("Invalid value stateFw %d for FirmwareUpdateStatus.", firmwareUpdateState);
        }
    }

    @NotNull
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(@NotNull SoftwareUpdateState swUpdateState) {
        switch (swUpdateState) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOAD_STARTED:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADING);
            case DELIVERED:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADED);
            case INSTALLED:
                return Optional.empty();
            default:
                throw new CodecException("Invalid value stateSw %d for SoftwareUpdateState.", swUpdateState);
        }
    }

    /**
     * FirmwareUpdateStatus {
     * DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
     */
    @NotNull
    public static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(@NotNull SoftwareUpdateResult softwareUpdateResult) {
        switch (softwareUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(OtaPackageUpdateStatus.DOWNLOADING);
            case SUCCESSFULLY_INSTALLED:
                return Optional.of(OtaPackageUpdateStatus.UPDATED);
            case SUCCESSFULLY_DOWNLOADED_VERIFIED:
                return Optional.of(OtaPackageUpdateStatus.VERIFIED);
            case NOT_ENOUGH_STORAGE:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case PACKAGE_CHECK_FAILURE:
            case UNSUPPORTED_PACKAGE_TYPE:
            case INVALID_URI:
            case UPDATE_ERROR:
            case INSTALL_FAILURE:
            case UN_INSTALL_FAILURE:
                return Optional.of(OtaPackageUpdateStatus.FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", softwareUpdateResult.name());
        }
    }

}
