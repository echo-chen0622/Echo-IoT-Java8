package org.echoiot.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.echoiot.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.echoiot.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.echoiot.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.echoiot.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.echoiot.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.echoiot.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.*;
import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.*;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MAttributesService implements LwM2MAttributesService {

    //TODO: add timeout logic
    private final AtomicInteger reqIdSeq = new AtomicInteger();
    @NotNull
    private final Map<Integer, SettableFuture<List<TransportProtos.TsKvProto>>> futures;

    @NotNull
    private final TransportService transportService;
    @NotNull
    private final LwM2mTransportServerHelper helper;
    @NotNull
    private final LwM2mClientContext clientContext;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2mUplinkMsgHandler uplinkHandler;
    @NotNull
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    @NotNull
    private final LwM2MTelemetryLogService logService;
    @NotNull
    private final LwM2MOtaUpdateService otaUpdateService;
    @NotNull
    private final LwM2mModelProvider modelProvider;

    @NotNull
    @Override
    public ListenableFuture<List<TransportProtos.TsKvProto>> getSharedAttributes(@NotNull LwM2mClient client, Collection<String> keys) {
        @NotNull SettableFuture<List<TransportProtos.TsKvProto>> future = SettableFuture.create();
        int requestId = reqIdSeq.incrementAndGet();
        futures.put(requestId, future);
        transportService.process(client.getSession(), TransportProtos.GetAttributeRequestMsg.newBuilder().setRequestId(requestId).
                addAllSharedAttributeNames(keys).build(), new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void msg) {

            }

            @Override
            public void onError(@NotNull Throwable e) {
                SettableFuture<List<TransportProtos.TsKvProto>> callback = futures.remove(requestId);
                if (callback != null) {
                    callback.setException(e);
                }
            }
        });
        return future;
    }

    @Override
    public void onGetAttributesResponse(@NotNull GetAttributeResponseMsg getAttributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        var callback = futures.remove(getAttributesResponse.getRequestId());
        if (callback != null) {
            callback.set(getAttributesResponse.getSharedAttributeListList());
        }
    }

    /**
     * Update - send request in change value resources in Client
     * 1. FirmwareUpdate:
     * - If msg.getSharedUpdatedList().forEach(tsKvProto -> {tsKvProto.getKv().getKey().indexOf(FIRMWARE_UPDATE_PREFIX, 0) == 0
     * 2. Shared Other AttributeUpdate
     * -- Path to resources from profile equal keyName or from ModelObject equal name
     * -- Only for resources:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     * 3. Delete - nothing
     *
     * @param msg -
     */
    @Override
    public void onAttributesUpdate(@NotNull TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
        if (msg.getSharedUpdatedCount() > 0 && lwM2MClient != null) {
            @Nullable String newFirmwareTitle = null;
            @Nullable String newFirmwareVersion = null;
            @Nullable String newFirmwareTag = null;
            @Nullable String newFirmwareUrl = null;
            @Nullable String newSoftwareTitle = null;
            @Nullable String newSoftwareVersion = null;
            @Nullable String newSoftwareTag = null;
            @Nullable String newSoftwareUrl = null;
            @NotNull List<TransportProtos.TsKvProto> otherAttributes = new ArrayList<>();
            for (@NotNull TransportProtos.TsKvProto tsKvProto : msg.getSharedUpdatedList()) {
                String attrName = tsKvProto.getKv().getKey();
                if (compareAttNameKeyOta(attrName)) {
                    if (FIRMWARE_TITLE.equals(attrName)) {
                        newFirmwareTitle = getStrValue(tsKvProto);
                    } else if (FIRMWARE_VERSION.equals(attrName)) {
                        newFirmwareVersion = getStrValue(tsKvProto);
                    } else if (FIRMWARE_TAG.equals(attrName)) {
                        newFirmwareTag = getStrValue(tsKvProto);
                    } else if (FIRMWARE_URL.equals(attrName)) {
                        newFirmwareUrl = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TITLE.equals(attrName)) {
                        newSoftwareTitle = getStrValue(tsKvProto);
                    } else if (SOFTWARE_VERSION.equals(attrName)) {
                        newSoftwareVersion = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TAG.equals(attrName)) {
                        newSoftwareTag = getStrValue(tsKvProto);
                    } else if (SOFTWARE_URL.equals(attrName)) {
                        newSoftwareUrl = getStrValue(tsKvProto);
                    }
                } else {
                    otherAttributes.add(tsKvProto);
                }
            }
            if (newFirmwareTitle != null || newFirmwareVersion != null) {
                otaUpdateService.onTargetFirmwareUpdate(lwM2MClient, newFirmwareTitle, newFirmwareVersion, Optional.ofNullable(newFirmwareUrl), Optional.ofNullable(newFirmwareTag));
            }
            if (newSoftwareTitle != null || newSoftwareVersion != null) {
                otaUpdateService.onTargetSoftwareUpdate(lwM2MClient, newSoftwareTitle, newSoftwareVersion, Optional.ofNullable(newSoftwareUrl), Optional.ofNullable(newSoftwareTag));
            }
            if (!otherAttributes.isEmpty()) {
                onAttributesUpdate(lwM2MClient, otherAttributes, true);
            }
        } else if (lwM2MClient == null) {
            log.error("OnAttributeUpdate, lwM2MClient is null");
        }
    }

    /**
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => send to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     */
    @Override
    public void onAttributesUpdate(@NotNull LwM2mClient lwM2MClient, @NotNull List<TransportProtos.TsKvProto> tsKvProtos, boolean logFailedUpdateOfNonChangedValue) {
        log.trace("[{}] onAttributesUpdate [{}]", lwM2MClient.getEndpoint(), tsKvProtos);
        @NotNull Map<String, TransportProtos.TsKvProto> attributesUpdate = new ConcurrentHashMap<>();
        tsKvProtos.forEach(tsKvProto -> {
            try {
                String pathIdVer = clientContext.getObjectIdByKeyNameFromProfile(lwM2MClient, tsKvProto.getKv().getKey());
                if (pathIdVer != null) {
                    // #1.1
                    if (lwM2MClient.getSharedAttributes().containsKey(pathIdVer)) {
                        if (tsKvProto.getTs() > lwM2MClient.getSharedAttributes().get(pathIdVer).getTs()) {
                            attributesUpdate.put(pathIdVer, tsKvProto);
                        }
                    } else {
                        attributesUpdate.put(pathIdVer, tsKvProto);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                        LOG_LWM2M_ERROR, e.getMessage());
                logService.log(lwM2MClient, logMsg);
            }
        });
        clientContext.update(lwM2MClient);
        // #2.1
        attributesUpdate.forEach((pathIdVer, tsKvProto) -> {
            @Nullable ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer, modelProvider);
            @Nullable Object newValProto = getValueFromKvProto(tsKvProto.getKv());
            @Nullable Object oldResourceValue = this.getResourceValueFormatKv(lwM2MClient, pathIdVer);
            if (!resourceModel.multiple || !(newValProto instanceof JsonElement)) {
                this.pushUpdateToClientIfNeeded(lwM2MClient, oldResourceValue, newValProto, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
            } else {
                try {
                    pushUpdateMultiToClientIfNeeded(lwM2MClient, resourceModel, (JsonElement) newValProto,
                            (Map<Integer, LwM2mResourceInstance>) oldResourceValue, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
                } catch (Exception e) {
                    log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                    String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                            LOG_LWM2M_ERROR, e.getMessage());
                    logService.log(lwM2MClient, logMsg);
                }
            }
        });
    }

    private void pushUpdateToClientIfNeeded(LwM2mClient lwM2MClient, @Nullable Object oldValue, @Nullable Object newValue,
                                            String versionedId, TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        if (newValue == null) {
            String logMsg = String.format("%s: Failed update resource versionedId - %s value - %s. New value is  bad",
                    LOG_LWM2M_ERROR, versionedId, "null");
            logService.log(lwM2MClient, logMsg);
            log.error("Failed update resource [{}] [{}]", versionedId, "null");
        } else if ((oldValue == null) || !valueEquals(newValue, oldValue)) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValue).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
            downlinkHandler.sendWriteReplaceRequest(lwM2MClient, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, lwM2MClient, versionedId) {
                @Override
                public void onSuccess(@NotNull WriteRequest request, @NotNull WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            String logMsg = String.format("%s: Didn't update the versionedId resource - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValue);
            logService.log(lwM2MClient, logMsg);
            log.warn("Didn't update resource [{}] [{}]. Value is not changed", versionedId, newValue);
        }
    }

    private void pushUpdateMultiToClientIfNeeded(LwM2mClient client, @NotNull ResourceModel resourceModel, @NotNull JsonElement newValProto,
                                                 @Nullable Map<Integer, LwM2mResourceInstance> valueOld, String versionedId,
                                                 TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        @NotNull Map<Integer, Object> newValues = convertMultiResourceValuesFromJson(newValProto, resourceModel.type, versionedId);
        if (newValues.size() > 0 && valueOld != null && valueOld.size() > 0) {
            valueOld.values().forEach((v) -> {
                if (newValues.containsKey(v.getId())) {
                    if (valueEquals(newValues.get(v.getId()), v.getValue())) {
                        newValues.remove(v.getId());
                    }
                }
            });
        }

        if (newValues.size() > 0) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValues).timeout(this.config.getTimeout()).build();
            downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId) {
                @Override
                public void onSuccess(@NotNull WriteRequest request, @NotNull WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            log.warn("Didn't update resource [{}] [{}]", versionedId, newValProto);
            String logMsg = String.format("%s: Didn't update resource versionedId - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValProto);
            logService.log(client, logMsg);
        }
    }

    /**
     * @param pathIdVer - path resource
     * @return - value of Resource into format KvProto or null
     */
    @Nullable
    private Object getResourceValueFormatKv(@NotNull LwM2mClient lwM2MClient, String pathIdVer) {
        @Nullable LwM2mResource resourceValue = LwM2MTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
        if (resourceValue != null) {
            ResourceModel.Type currentType = resourceValue.getType();
            ResourceModel.Type expectedType = LwM2mTransportServerHelper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
            if (!resourceValue.isMultiInstances()) {
                return LwM2mValueConverterImpl.getInstance().convertValue(resourceValue.getValue(), currentType, expectedType,
                        new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
            } else if (resourceValue.getInstances().size() > 0) {
                return resourceValue.getInstances();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getStrValue(@NotNull TransportProtos.TsKvProto tsKvProto) {
        return tsKvProto.getKv().getStringV();
    }
}
