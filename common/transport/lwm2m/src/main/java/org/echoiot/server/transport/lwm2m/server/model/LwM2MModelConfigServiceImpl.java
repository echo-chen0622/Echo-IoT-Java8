package org.echoiot.server.transport.lwm2m.server.model;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.echoiot.server.queue.util.AfterStartUp;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.echoiot.server.transport.lwm2m.server.downlink.*;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2MModelConfigServiceImpl implements LwM2MModelConfigService {

    @Resource
    private TbLwM2MModelConfigStore modelStore;

    @Resource
    @Lazy
    private LwM2mDownlinkMsgHandler downlinkMsgHandler;
    @Resource
    @Lazy
    private LwM2mUplinkMsgHandler uplinkMsgHandler;
    @Resource
    @Lazy
    private LwM2mClientContext clientContext;

    @Resource
    private LwM2MTelemetryLogService logService;

    private ConcurrentMap<String, LwM2MModelConfig> currentModelConfigs;

    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
    private void init() {
        List<LwM2MModelConfig> models = modelStore.getAll();
        log.debug("Fetched model configs: {}", models);
        currentModelConfigs = models.stream()
                .collect(Collectors.toConcurrentMap(LwM2MModelConfig::getEndpoint, m -> m));
    }

    @Override
    public void sendUpdates(LwM2mClient lwM2mClient) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(lwM2mClient.getEndpoint());
        if (modelConfig == null || modelConfig.isEmpty()) {
            return;
        }

        doSend(lwM2mClient, modelConfig);
    }

    public void sendUpdates(LwM2mClient lwM2mClient, LwM2MModelConfig newModelConfig) {
        String endpoint = lwM2mClient.getEndpoint();
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig == null || modelConfig.isEmpty()) {
            modelConfig = newModelConfig;
            currentModelConfigs.put(endpoint, modelConfig);
        } else {
            modelConfig.merge(newModelConfig);
        }

        if (lwM2mClient.isAsleep()) {
            modelStore.put(modelConfig);
        } else {
            doSend(lwM2mClient, modelConfig);
        }
    }

    private void doSend(LwM2mClient lwM2mClient, LwM2MModelConfig modelConfig) {
        log.trace("Send LwM2M Model updates: [{}]", modelConfig);

        String endpoint = lwM2mClient.getEndpoint();

        Map<String, ObjectAttributes> attrToAdd = modelConfig.getAttributesToAdd();
        attrToAdd.forEach((id, attributes) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                                                                                 .attributes(attributes)
                                                                                 .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToAdd.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> attrToRemove = modelConfig.getAttributesToRemove();
        attrToRemove.forEach((id) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(new ObjectAttributes())
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToRemove.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> toRead = modelConfig.getToRead();
        toRead.forEach(id -> {
            TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(id)
                                                           .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendReadRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toRead.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MReadCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toObserve = modelConfig.getToObserve();
        toObserve.forEach(id -> {
            TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MObserveCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toCancelObserve = modelConfig.getToCancelObserve();
        toCancelObserve.forEach(id -> {
            TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendCancelObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toCancelObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MCancelObserveCallback(logService, lwM2mClient, id))
            );
        });
    }

    private <R, T> DownlinkRequestCallback<R, T> createDownlinkProxyCallback(Runnable processRemove, DownlinkRequestCallback<R, T> callback) {
        return new DownlinkRequestCallback<>() {
            @Override
            public void onSuccess(R request, T response) {
                processRemove.run();
                callback.onSuccess(request, response);
            }

            @Override
            public void onValidationError(String params, String msg) {
                processRemove.run();
                callback.onValidationError(params, msg);
            }

            @Override
            public void onError(String params, Exception e) {
                try {
                    if (e instanceof TimeoutException) {
                        return;
                    }
                    processRemove.run();
                } finally {
                    callback.onError(params, e);
                }
            }

        };
    }

    @Override
    public void persistUpdates(String endpoint) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig != null && !modelConfig.isEmpty()) {
            modelStore.put(modelConfig);
        }
    }

    @Override
    public void removeUpdates(String endpoint) {
        currentModelConfigs.remove(endpoint);
    }

    @PreDestroy
    private void destroy() {
        currentModelConfigs.values().forEach(model -> {
            if (model != null && !model.isEmpty()) {
                modelStore.put(model);
            }
        });
    }
}
