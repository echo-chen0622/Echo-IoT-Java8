package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.server.LwM2mTransportContext;
import org.echoiot.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.echoiot.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.echoiot.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.rpc.composite.RpcWriteCompositeRequest;
import org.echoiot.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.*;
import static org.eclipse.leshan.core.attributes.Attribute.*;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mDownlinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mDownlinkMsgHandler {

    public LwM2mValueConverterImpl converter;

    @NotNull
    private final LwM2mTransportContext context;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final LwM2MTelemetryLogService logService;
    @NotNull
    private final LwM2mClientContext clientContext;
    @NotNull
    private final LwM2mVersionedModelProvider modelProvider;

    @PostConstruct
    public void init() {
        super.init();
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected int getExecutorSize() {
        return config.getDownlinkPoolSize();
    }

    @NotNull
    @Override
    protected String getExecutorName() {
        return "LwM2M Downlink";
    }

    @Override
    public void sendReadRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MReadRequest request, @NotNull DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        validateVersionedId(client, request);
        @NotNull ReadRequest downlink = new ReadRequest(getRequestContentFormat(client, request.getVersionedId(), modelProvider), request.getObjectId());
        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
    }

    @Override
    public void sendReadCompositeRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MReadCompositeRequest request,
                                         @NotNull DownlinkRequestCallback<ReadCompositeRequest, ReadCompositeResponse> callback, ContentFormat compositeContentFormat) {
        try {
            @NotNull ReadCompositeRequest downlink = new ReadCompositeRequest(compositeContentFormat, compositeContentFormat, request.getObjectIds());
            sendCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendObserveRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MObserveRequest request, @NotNull DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        try {
            validateVersionedId(client, request);
            @NotNull LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
            //TODO: should be able to use CompositeObservation
            if (observations.stream().noneMatch(observation -> ((SingleObservation)observation).getPath().equals(resultIds))) {
                ObserveRequest downlink;
                ContentFormat contentFormat = getReadRequestContentFormat(client, request, modelProvider);
                if (resultIds.isResource()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId());
                } else if (resultIds.isObjectInstance()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId());
                } else {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId());
                }
                log.info("[{}] Send observation: {}.", client.getEndpoint(), request.getVersionedId());
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError(resultIds.toString(), "Observation is already registered!");
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendObserveAllRequest(@NotNull LwM2mClient client, TbLwM2MObserveAllRequest request, @NotNull DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        //TODO: should be able to use CompositeObservation
        @NotNull Set<String> paths = observations.stream().map(observation -> ((SingleObservation)observation).getPath().toString()).collect(Collectors.toUnmodifiableSet());
        callback.onSuccess(request, paths);
    }

    @Override
    public void sendDiscoverAllRequest(@NotNull LwM2mClient client, TbLwM2MDiscoverAllRequest request, @NotNull DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<Link>> callback) {
        callback.onSuccess(request, Arrays.asList(client.getRegistration().getSortedObjectLinks()));
    }

    @Override
    public void sendExecuteRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MExecuteRequest request, @NotNull DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback) {
        try {
            validateVersionedId(client, request);
            @NotNull LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(request.getVersionedId()));
            @Nullable ResourceModel resourceModelExecute = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelExecute == null) {
                @NotNull LwM2mModel model = createModelsDefault();
                if (pathIds.isResource()) {
                    resourceModelExecute = model.getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
                }
            }
            if (resourceModelExecute == null) {
                callback.onValidationError(request.toString(), "ResourceModel with " + request.getVersionedId() +
                        " is absent in system. Need ddd Lwm2m Model with id=" + pathIds.getObjectId() + " ver=" +
                        getVerFromPathIdVerOrId(request.getVersionedId()) + " to profile.");
            } else if (resourceModelExecute.operations.isExecutable()) {
                ExecuteRequest downlink;
                if (request.getParams() != null && !resourceModelExecute.multiple) {
                    downlink = new ExecuteRequest(request.getObjectId(), (String) this.converter.convertValue(request.getParams(),
                            resourceModelExecute.type, ResourceModel.Type.STRING, new LwM2mPath(request.getObjectId())));
                } else {
                    downlink = new ExecuteRequest(request.getObjectId());
                }
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError(request.toString(), "Resource with " + request.getVersionedId() + " is not executable.");
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendDeleteRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MDeleteRequest request, @NotNull DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback) {
        try {
            validateVersionedId(client, request);
            sendSimpleRequest(client, new DeleteRequest(request.getObjectId()), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendCancelObserveRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MCancelObserveRequest request, @NotNull DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback) {
        validateVersionedId(client, request);
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration(), request.getObjectId());
        callback.onSuccess(request, observeCancelCnt);
    }

    @Override
    public void sendCancelAllRequest(@NotNull LwM2mClient client, TbLwM2MCancelAllRequest request, @NotNull DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration());
        callback.onSuccess(request, observeCancelCnt);
    }

    @Override
    public void sendDiscoverRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MDiscoverRequest request, @NotNull DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback) {
        validateVersionedId(client, request);
        sendSimpleRequest(client, new DiscoverRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    /**
     * Example # 1:
     * AttributeSet attributes = new AttributeSet(new Attribute(Attribute.MINIMUM_PERIOD, 10L),
     * new Attribute(Attribute.MAXIMUM_PERIOD, 100L));
     * WriteAttributesRequest requestTest = new WriteAttributesRequest(3, 0, 14, attributes);
     * sendSimpleRequest(client, requestTest, request.getTimeout(), callback);
     * <p>
     * Example # 2
     * Dimension and Object version are read only attributes.
     * addAttribute(attributes, DIMENSION, params.getDim(), dim -> dim >= 0 && dim <= 255);
     * addAttribute(attributes, OBJECT_VERSION, params.getVer(), StringUtils::isNotEmpty, Function.identity());
     */
    @Override
    public void sendWriteAttributesRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MWriteAttributesRequest request, @NotNull DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback) {
        try {
            validateVersionedId(client, request);
            if (request.getAttributes() == null) {
                throw new IllegalArgumentException("Attributes to write are not specified!");
            }
            @NotNull ObjectAttributes params = request.getAttributes();
            @NotNull List<Attribute> attributes = new LinkedList<>();
            addAttribute(attributes, MAXIMUM_PERIOD, params.getPmax());
            addAttribute(attributes, MINIMUM_PERIOD, params.getPmin());
            addAttribute(attributes, GREATER_THAN, params.getGt());
            addAttribute(attributes, LESSER_THAN, params.getLt());
            addAttribute(attributes, STEP, params.getSt());
            @NotNull AttributeSet attributeSet = new AttributeSet(attributes);
            sendSimpleRequest(client, new WriteAttributesRequest(request.getObjectId(), attributeSet), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendWriteReplaceRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MWriteReplaceRequest request, @NotNull DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        @NotNull LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        if (resultIds.isResource() || resultIds.isResourceInstance()) {
            validateVersionedId(client, request);
            @Nullable ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelWrite != null) {
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                try {
                    @Nullable WriteRequest downlink = null;
                    @NotNull String msgError = "";
                    if (resourceModelWrite.multiple) {
                        try {
                            @NotNull Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(request.getValue(), resourceModelWrite.type, request.getObjectId());
                            downlink = new WriteRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                    value, resourceModelWrite.type);
                        } catch (Exception e) {
                        }
                    }
                    if (downlink == null) {
                        try {
                            downlink = this.getWriteRequestSingleResource(resourceModelWrite.type, contentFormat,
                                    resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(), request.getValue());
                        } catch (Exception e) {
                            msgError = "Resource id=" + resultIds + ", value = " + request.getValue() +
                                       ", class = " + request.getValue().getClass().getSimpleName() + ". Format value is bad. Value for this Single Resource must be " + resourceModelWrite.type + "!";
                        }
                    }
                    if (downlink != null) {
                        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                    } else {
                        callback.onValidationError(toString(request), msgError);
                    }
                } catch (Exception e) {
                    callback.onError(toString(request), e);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + " is not configured in the device profile!");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + ". This operation can only be used for Resource or ResourceInstance!");
        }
    }

    @Override
    public void sendWriteCompositeRequest(@NotNull LwM2mClient client, @NotNull RpcWriteCompositeRequest rpcWriteCompositeRequest,
                                          @NotNull DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback, ContentFormat contentFormatComposite) {
        try {
            @NotNull WriteCompositeRequest downlink = new WriteCompositeRequest(contentFormatComposite, rpcWriteCompositeRequest.getNodes());
            //TODO: replace config.getTimeout();
            sendWriteCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(rpcWriteCompositeRequest.toString(), e.getMessage());
        } catch (Exception e) {
            callback.onError(toString(rpcWriteCompositeRequest), e);
        }
    }

    @Override
    public void sendWriteUpdateRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MWriteUpdateRequest request, @NotNull DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        try {
            @NotNull LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            if (resultIds.isObjectInstance() || resultIds.isResource()) {
                validateVersionedId(client, request);
                @Nullable WriteRequest downlink = null;
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                @NotNull String msgError = "";
                if (resultIds.isObjectInstance()) {
                    /*
                     *  params = "{\"id\":0,\"value\":[{\"id\":14,\"value\":\"+5\"},{\"id\":15,\"value\":\"+9\"}]}"
                     *  int rscId = resultIds.getObjectInstanceId();
                     *  contentFormat – Format of the payload (TLV or JSON).
                     */
                    @NotNull Collection<LwM2mResource> resources = client.getNewResourcesForInstance(request.getVersionedId(),
                                                                                                     request.getValue(), modelProvider, this.converter);
                    if (resources.size() > 0) {
                        downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                resultIds.getObjectInstanceId(), resources);
                    } else {
                        msgError = " No resources to update!";
                    }
                } else if (resultIds.isResource()) {
                    @Nullable ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
                    if (resourceModelWrite != null) {
                        if (resourceModelWrite.multiple) {
                            try {
                                @NotNull Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(request.getValue(), resourceModelWrite.type, request.getObjectId());
                                downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                        resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                        value, resourceModelWrite.type);
                            } catch (Exception e1) {
                                msgError = " Resource id=" + resultIds +
                                           ", class = " + request.getValue().getClass().getSimpleName() +
                                           ", value = " + request.getValue() + " is bad. " +
                                           "Value of Multi-Instance Resource must be in Json format!";
                            }
                        }
                    } else {
                        msgError = " Resource " + request.getVersionedId() + " is not configured in the device profile!";
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                            ". This operation can only be used for ObjectInstance or Multi-Instance Resource !" + msgError);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                        ". This operation can only be used for ObjectInstance or Resource (multiple)");
            }
        } catch (Exception e) {
            callback.onValidationError(toString(request), e.getMessage());
        }
    }

    public void sendCreateRequest(@NotNull LwM2mClient client, @NotNull TbLwM2MCreateRequest request, @NotNull DownlinkRequestCallback<CreateRequest, CreateResponse> callback) {
        validateVersionedId(client, request);
        @Nullable CreateRequest downlink = null;
        @NotNull LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        @Nullable ObjectModel objectModel = client.getObjectModel(request.getVersionedId(), modelProvider);
        // POST /{Object ID}/{Object Instance ID} && Resources is Mandatory
        if (objectModel != null) {
            if (objectModel.multiple) {

                // LwM2M CBOR, SenML CBOR, SenML JSON, or TLV (see [LwM2M-CORE])
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                if (resultIds.isObject() || resultIds.isObjectInstance()) {
                    Collection<LwM2mResource> resources;
                    if (resultIds.isObject()) {
                        if (request.getValue() != null) {
                            resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), resources);
                        } else if (request.getNodes() != null && request.getNodes().size() > 0) {
                            @NotNull Set<LwM2mObjectInstance> instances = ConcurrentHashMap.newKeySet();
                            request.getNodes().forEach((key, value) -> {
                                @NotNull Collection<LwM2mResource> resourcesForInstance = client.getNewResourcesForInstance(request.getVersionedId(), value, modelProvider, this.converter);
                                @NotNull LwM2mObjectInstance instance = new LwM2mObjectInstance(Integer.parseInt(key), resourcesForInstance);
                                instances.add(instance);
                            });
                            @NotNull LwM2mObjectInstance[] instanceArrays = instances.toArray(new LwM2mObjectInstance[instances.size()]);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instanceArrays);
                        }

                    } else {
                        resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                        @NotNull LwM2mObjectInstance instance = new LwM2mObjectInstance(resultIds.getObjectInstanceId(), resources);
                        downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instance);
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Path " + request.getVersionedId() +
                            ". Object must be Multiple !");
                }
            } else {
                throw new IllegalArgumentException("Path " + request.getVersionedId() + ". Object must be Multiple !");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                    " is not configured in the device profile!");
        }
    }

    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendSimpleRequest(@NotNull LwM2mClient client, @NotNull R request, long timeoutInMs, @NotNull DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPath().toString());
    }

    private <R extends CompositeDownlinkRequest<T>, T extends LwM2mResponse> void sendCompositeRequest(@NotNull LwM2mClient client, @NotNull R request, long timeoutInMs, @NotNull DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPaths().toString());
    }

    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void sendRequest(@NotNull LwM2mClient client, @NotNull R request, long timeoutInMs, @NotNull DownlinkRequestCallback<R, T> callback, @NotNull Function<R, String> pathToStringFunction) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), pathToStringFunction.apply(request)));
            if (!callback.onSent(request)) {
                return;
            }

            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        callback.onSuccess(request, response);
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendWriteCompositeRequest(@NotNull LwM2mClient client, @NotNull WriteCompositeRequest request, long timeoutInMs, @NotNull DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), request.getPaths()));
            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        if (response.isSuccess()) {
                            callback.onSuccess(request, response);
                        } else {
                            callback.onValidationError(request.getNodes().values().toString(), response.getErrorMessage());
                        }
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void handleDownlinkError(@NotNull LwM2mClient client, R request, @NotNull DownlinkRequestCallback<R, T> callback, Exception e) {
        log.trace("[{}] Received downlink error: {}.", client.getEndpoint(), e);
        client.updateLastUplinkTime();
        executor.submit(() -> {
            if (e instanceof TimeoutException || e instanceof ClientSleepingException) {
                log.trace("[{}] Received {}, client is probably sleeping", client.getEndpoint(), e.getClass().getSimpleName());
                clientContext.asleep(client);
            } else {
                log.trace("[{}] Received {}", client.getEndpoint(), e.getClass().getSimpleName());
            }
            callback.onError(toString(request), e);
        });
    }

    @NotNull
    private WriteRequest getWriteRequestSingleResource(@NotNull ResourceModel.Type type, ContentFormat contentFormat, int objectId, int instanceId, int resourceId, @NotNull Object value) {
        switch (type) {
            case STRING:    // String
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, value.toString());
            case INTEGER:   // Long
                final long valueInt = Integer.toUnsignedLong(Integer.parseInt(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueInt);
            case OBJLNK:    // ObjectLink
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, ObjectLink.fromPath(value.toString()));
            case BOOLEAN:   // Boolean
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Boolean.parseBoolean(value.toString()));
            case FLOAT:     // Double
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Double.parseDouble(value.toString()));
            case TIME:      // Date
                @NotNull Date date = new Date(Long.decode(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, date);
            case OPAQUE:    // byte[] value, base64
                byte[] valueRequest;
                if (value instanceof byte[]) {
                    valueRequest = (byte[]) value;
                } else {
                    valueRequest = Hex.decodeHex(value.toString().toCharArray());
                }
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueRequest);
            default:
                throw new IllegalArgumentException("Not supported type:" + type.name());
        }
    }

    private static <T> void addAttribute(@NotNull List<Attribute> attributes, @NotNull String attributeName, T value) {
        addAttribute(attributes, attributeName, value, null, null);
    }

    private static <T> void addAttribute(@NotNull List<Attribute> attributes, @NotNull String attributeName, T value, Function<T, ?> converter) {
        addAttribute(attributes, attributeName, value, null, converter);
    }

    private static <T> void addAttribute(@NotNull List<Attribute> attributes, @NotNull String attributeName, @Nullable T value, @Nullable Predicate<T> filter, @Nullable Function<T, ?> converter) {
        if (value != null && ((filter == null) || filter.test(value))) {
            attributes.add(new Attribute(attributeName, converter != null ? converter.apply(value) : value));
        }
    }

    private static <T extends HasContentFormat & HasVersionedId> ContentFormat getReadRequestContentFormat(@NotNull LwM2mClient client, @NotNull T request, @NotNull LwM2mModelProvider modelProvider) {
        if (request.getRequestContentFormat().isPresent()) {
            return request.getRequestContentFormat().get();
        } else {
            return getRequestContentFormat(client, request.getVersionedId(), modelProvider);
        }
    }

    private static ContentFormat getWriteRequestContentFormat(@NotNull LwM2mClient client, TbLwM2MDownlinkRequest request, @NotNull LwM2mModelProvider modelProvider) {
        if (request instanceof TbLwM2MWriteReplaceRequest && ((TbLwM2MWriteReplaceRequest) request).getContentFormat() != null) {
            return ((TbLwM2MWriteReplaceRequest) request).getContentFormat();
        } else if (request instanceof TbLwM2MWriteUpdateRequest && ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat() != null) {
            return ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat();
        } else {
            @Nullable String versionedId = null;
            if (request instanceof TbLwM2MWriteReplaceRequest) {
                versionedId = ((TbLwM2MWriteReplaceRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MWriteUpdateRequest) {
                versionedId = ((TbLwM2MWriteUpdateRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MCreateRequest) {
                versionedId = ((TbLwM2MCreateRequest) request).getVersionedId();
            }
            return getRequestContentFormat(client, versionedId, modelProvider);
        }
    }

    private static ContentFormat getRequestContentFormat(@NotNull LwM2mClient client, @NotNull String versionedId, @NotNull LwM2mModelProvider modelProvider) {
        @NotNull LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(versionedId));
        if (pathIds.isResource() || pathIds.isResourceInstance()) {
            @Nullable ResourceModel resourceModel = client.getResourceModel(versionedId, modelProvider);
            if (resourceModel != null && (pathIds.isResourceInstance() || (pathIds.isResource() && !resourceModel.multiple))) {
                if (OBJLNK.equals(resourceModel.type)) {
                    return ContentFormat.LINK;
                } else if (OPAQUE.equals(resourceModel.type)) {
                    return ContentFormat.OPAQUE;
                } else {
                    return findFirst(client.getClientSupportContentFormats(), client.getDefaultContentFormat(), ContentFormat.CBOR, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON);
                }
            } else {
                return getContentFormatForComplex(client);
            }
        } else {
            return getContentFormatForComplex(client);
        }
    }

    @NotNull
    private static ContentFormat getContentFormatForComplex(@NotNull LwM2mClient client) {
        if (LwM2m.LwM2mVersion.V1_0.equals(client.getRegistration().getLwM2mVersion())) {
            return ContentFormat.TLV;
        } else if (LwM2m.LwM2mVersion.V1_1.equals(client.getRegistration().getLwM2mVersion())) {
            ContentFormat result = findFirst(client.getClientSupportContentFormats(), null, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON, ContentFormat.TLV, ContentFormat.JSON);
            if (result != null) {
                return result;
            } else {
                throw new RuntimeException("The client does not support any of SenML CBOR, SenML JSON, TLV or JSON formats. Can't send complex requests. Try using singe-instance requests.");
            }
        } else {
            throw new RuntimeException("The version " + client.getRegistration().getLwM2mVersion() + " is not supported!");
        }
    }

    private static ContentFormat findFirst(@NotNull Set<ContentFormat> supported, ContentFormat defaultValue, @NotNull ContentFormat... desiredFormats) {
        for (ContentFormat contentFormat : desiredFormats) {
            if (supported.contains(contentFormat)) {
                return contentFormat;
            }
        }
        return defaultValue;
    }

    private <R> String toString(@Nullable R request) {
        try {
            return request != null ? request.toString() : "";
        } catch (Exception e) {
            log.debug("Failed to convert request to string", e);
            return request.getClass().getSimpleName();
        }
    }
}
