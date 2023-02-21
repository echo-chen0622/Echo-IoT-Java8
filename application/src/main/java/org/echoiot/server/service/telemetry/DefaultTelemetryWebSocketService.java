package org.echoiot.server.service.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotExecutors;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.util.TenantRateLimitException;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.AccessValidator;
import org.echoiot.server.service.security.ValidationCallback;
import org.echoiot.server.service.security.ValidationResult;
import org.echoiot.server.service.security.ValidationResultCode;
import org.echoiot.server.service.security.model.UserPrincipal;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.subscription.*;
import org.echoiot.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.echoiot.server.service.telemetry.cmd.v1.*;
import org.echoiot.server.service.telemetry.cmd.v2.*;
import org.echoiot.server.service.telemetry.exception.UnauthorizedException;
import org.echoiot.server.service.telemetry.sub.SubscriptionErrorCode;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Echo on 27.03.18.
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultTelemetryWebSocketService implements TelemetryWebSocketService {

    public static final int NUMBER_OF_PING_ATTEMPTS = 3;

    private static final int DEFAULT_LIMIT = 100;
    private static final Aggregation DEFAULT_AGGREGATION = Aggregation.NONE;
    private static final int UNKNOWN_SUBSCRIPTION_ID = 0;
    private static final String PROCESSING_MSG = "[{}] Processing: {}";
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final String FAILED_TO_FETCH_DATA = "Failed to fetch data!";
    private static final String FAILED_TO_FETCH_ATTRIBUTES = "Failed to fetch attributes!";
    private static final String SESSION_META_DATA_NOT_FOUND = "Session meta-data not found!";
    private static final String FAILED_TO_PARSE_WS_COMMAND = "Failed to parse websocket command!";

    private final ConcurrentMap<String, WsSessionMetaData> wsSessionsMap = new ConcurrentHashMap<>();

    @Resource
    private TbLocalSubscriptionService oldSubService;

    @Resource
    private TbEntityDataSubscriptionService entityDataSubService;

    @Resource
    private TelemetryWebSocketMsgEndpoint msgEndpoint;

    @Resource
    private AccessValidator accessValidator;

    @Resource
    private AttributesService attributesService;

    @Resource
    private TimeseriesService tsService;

    @Resource
    private TbServiceInfoProvider serviceInfoProvider;

    @Resource
    private TbTenantProfileCache tenantProfileCache;

    @Value("${server.ws.ping_timeout:30000}")
    private long pingTimeout;

    private final ConcurrentMap<TenantId, Set<String>> tenantSubscriptionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, Set<String>> customerSubscriptionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> regularUserSubscriptionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> publicUserSubscriptionsMap = new ConcurrentHashMap<>();

    private ExecutorService executor;
    private String serviceId;

    private ScheduledExecutorService pingExecutor;

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        executor = EchoiotExecutors.newWorkStealingPool(50, getClass());

        pingExecutor = Executors.newSingleThreadScheduledExecutor(EchoiotThreadFactory.forName("telemetry-web-socket-ping"));
        pingExecutor.scheduleWithFixedDelay(this::sendPing, pingTimeout / NUMBER_OF_PING_ATTEMPTS, pingTimeout / NUMBER_OF_PING_ATTEMPTS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (pingExecutor != null) {
            pingExecutor.shutdownNow();
        }

        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void handleWebSocketSessionEvent(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull SessionEvent event) {
        String sessionId = sessionRef.getSessionId();
        log.debug(PROCESSING_MSG, sessionId, event);
        switch (event.getEventType()) {
            case ESTABLISHED:
                wsSessionsMap.put(sessionId, new WsSessionMetaData(sessionRef));
                break;
            case ERROR:
                log.debug("[{}] Unknown websocket session error: {}. ", sessionId, event.getError().orElse(null));
                break;
            case CLOSED:
                wsSessionsMap.remove(sessionId);
                oldSubService.cancelAllSessionSubscriptions(sessionId);
                entityDataSubService.cancelAllSessionSubscriptions(sessionId);
                processSessionClose(sessionRef);
                break;
        }
    }

    @Override
    public void handleWebSocketMsg(@NotNull TelemetryWebSocketSessionRef sessionRef, String msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing: {}", sessionRef.getSessionId(), msg);
        }

        try {
            TelemetryPluginCmdsWrapper cmdsWrapper = jsonMapper.readValue(msg, TelemetryPluginCmdsWrapper.class);
            if (cmdsWrapper != null) {
                if (cmdsWrapper.getAttrSubCmds() != null) {
                    cmdsWrapper.getAttrSubCmds().forEach(cmd -> {
                        if (processSubscription(sessionRef, cmd)) {
                            handleWsAttributesSubscriptionCmd(sessionRef, cmd);
                        }
                    });
                }
                if (cmdsWrapper.getTsSubCmds() != null) {
                    cmdsWrapper.getTsSubCmds().forEach(cmd -> {
                        if (processSubscription(sessionRef, cmd)) {
                            handleWsTimeseriesSubscriptionCmd(sessionRef, cmd);
                        }
                    });
                }
                if (cmdsWrapper.getHistoryCmds() != null) {
                    cmdsWrapper.getHistoryCmds().forEach(cmd -> handleWsHistoryCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getEntityDataCmds() != null) {
                    cmdsWrapper.getEntityDataCmds().forEach(cmd -> handleWsEntityDataCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getAlarmDataCmds() != null) {
                    cmdsWrapper.getAlarmDataCmds().forEach(cmd -> handleWsAlarmDataCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getEntityCountCmds() != null) {
                    cmdsWrapper.getEntityCountCmds().forEach(cmd -> handleWsEntityCountCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getEntityDataUnsubscribeCmds() != null) {
                    cmdsWrapper.getEntityDataUnsubscribeCmds().forEach(cmd -> handleWsDataUnsubscribeCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getAlarmDataUnsubscribeCmds() != null) {
                    cmdsWrapper.getAlarmDataUnsubscribeCmds().forEach(cmd -> handleWsDataUnsubscribeCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getEntityCountUnsubscribeCmds() != null) {
                    cmdsWrapper.getEntityCountUnsubscribeCmds().forEach(cmd -> handleWsDataUnsubscribeCmd(sessionRef, cmd));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to decode subscription cmd: {}", e.getMessage(), e);
            sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(UNKNOWN_SUBSCRIPTION_ID, SubscriptionErrorCode.BAD_REQUEST, FAILED_TO_PARSE_WS_COMMAND));
        }
    }

    private void handleWsEntityDataCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityDataCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsEntityCountCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityCountCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsAlarmDataCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull AlarmDataCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsDataUnsubscribeCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull UnsubscribeCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)) {
            entityDataSubService.cancelSubscription(sessionRef.getSessionId(), cmd);
        }
    }

    @Override
    public void sendWsMsg(String sessionId, @NotNull TelemetrySubscriptionUpdate update) {
        sendWsMsg(sessionId, update.getSubscriptionId(), update);
    }

    @Override
    public void sendWsMsg(String sessionId, @NotNull CmdUpdate update) {
        sendWsMsg(sessionId, update.getCmdId(), update);
    }

    private <T> void sendWsMsg(String sessionId, int cmdId, T update) {
        WsSessionMetaData md = wsSessionsMap.get(sessionId);
        if (md != null) {
            sendWsMsg(md.getSessionRef(), cmdId, update);
        }
    }

    @Override
    public void close(String sessionId, CloseStatus status) {
        WsSessionMetaData md = wsSessionsMap.get(sessionId);
        if (md != null) {
            try {
                msgEndpoint.close(md.getSessionRef(), status);
            } catch (IOException e) {
                log.warn("[{}] Failed to send session close: {}", sessionId, e);
            }
        }
    }

    private void processSessionClose(@NotNull TelemetryWebSocketSessionRef sessionRef) {
        @org.jetbrains.annotations.Nullable var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration != null) {
            @NotNull String sessionId = "[" + sessionRef.getSessionId() + "]";

            if (tenantProfileConfiguration.getMaxWsSubscriptionsPerTenant() > 0) {
                @NotNull Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (tenantSubscriptions) {
                    tenantSubscriptions.removeIf(subId -> subId.startsWith(sessionId));
                }
            }
            if (sessionRef.getSecurityCtx().isCustomerUser()) {
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerCustomer() > 0) {
                    @NotNull Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (customerSessions) {
                        customerSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerRegularUser() > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    @NotNull Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (regularUserSessions) {
                        regularUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerPublicUser() > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    @NotNull Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (publicUserSessions) {
                        publicUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
            }
        }
    }

    private boolean processSubscription(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull SubscriptionCmd cmd) {
        @org.jetbrains.annotations.Nullable var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) return true;

        @NotNull String subId = "[" + sessionRef.getSessionId() + "]:[" + cmd.getCmdId() + "]";
        try {
            if (tenantProfileConfiguration.getMaxWsSubscriptionsPerTenant() > 0) {
                @NotNull Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (tenantSubscriptions) {
                    if (cmd.isUnsubscribe()) {
                        tenantSubscriptions.remove(subId);
                    } else if (tenantSubscriptions.size() < tenantProfileConfiguration.getMaxWsSubscriptionsPerTenant()) {
                        tenantSubscriptions.add(subId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start subscription. Max tenant subscriptions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                        msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max tenant subscriptions limit reached!"));
                        return false;
                    }
                }
            }

            if (sessionRef.getSecurityCtx().isCustomerUser()) {
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerCustomer() > 0) {
                    @NotNull Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (customerSessions) {
                        if (cmd.isUnsubscribe()) {
                            customerSessions.remove(subId);
                        } else if (customerSessions.size() < tenantProfileConfiguration.getMaxWsSubscriptionsPerCustomer()) {
                            customerSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max customer subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max customer subscriptions limit reached"));
                            return false;
                        }
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerRegularUser() > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    @NotNull Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (regularUserSessions) {
                        if (regularUserSessions.size() < tenantProfileConfiguration.getMaxWsSubscriptionsPerRegularUser()) {
                            regularUserSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max regular user subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max regular user subscriptions limit reached"));
                            return false;
                        }
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerPublicUser() > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    @NotNull Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (publicUserSessions) {
                        if (publicUserSessions.size() < tenantProfileConfiguration.getMaxWsSubscriptionsPerPublicUser()) {
                            publicUserSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max public user subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max public user subscriptions limit reached"));
                            return false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("[{}] Failed to send session close: {}", sessionRef.getSessionId(), e);
            return false;
        }
        return true;
    }

    private void handleWsAttributesSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull AttributesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                log.debug("[{}] fetching latest attributes ({}) values for device: {}", sessionId, cmd.getKeys(), entityId);
                @NotNull Optional<Set<String>> keysOptional = getKeys(cmd);
                if (keysOptional.isPresent()) {
                    @NotNull List<String> keys = new ArrayList<>(keysOptional.get());
                    handleWsAttributesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId, keys);
                } else {
                    handleWsAttributesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsAttributesSubscriptionByKeys(@NotNull TelemetryWebSocketSessionRef sessionRef,
                                                      @NotNull AttributesSubscriptionCmd cmd, String sessionId, @NotNull EntityId entityId,
                                                      @NotNull List<String> keys) {
        @NotNull FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull List<AttributeKvEntry> data) {
                @NotNull List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), attributesData));

                @NotNull Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, 0L));
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                @NotNull TbAttributeSubscriptionScope scope = StringUtils.isEmpty(cmd.getScope()) ? TbAttributeSubscriptionScope.ANY_SCOPE : TbAttributeSubscriptionScope.valueOf(cmd.getScope());

                TbAttributeSubscription sub = TbAttributeSubscription.builder()
                                                                     .serviceId(serviceId)
                                                                     .sessionId(sessionId)
                                                                     .subscriptionId(cmd.getCmdId())
                                                                     .tenantId(sessionRef.getSecurityCtx().getTenantId())
                                                                     .entityId(entityId)
                                                                     .allKeys(false)
                                                                     .keyStates(subState)
                                                                     .scope(scope)
                                                                     .updateConsumer(DefaultTelemetryWebSocketService.this::sendWsMsg)
                                                                     .build();
                oldSubService.addSubscription(sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                TelemetrySubscriptionUpdate update;
                if (e instanceof UnauthorizedException) {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_ATTRIBUTES);
                }
                sendWsMsg(sessionRef, update);
            }
        };

        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, keys, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, cmd.getScope(), keys, callback));
        }
    }

    private void handleWsHistoryCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull GetHistoryCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                                                                                          SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty() || cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getKeys() == null || cmd.getKeys().isEmpty()) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Keys are empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
        @NotNull List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
        @NotNull List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), getAggregation(cmd.getAgg())))
                                                   .collect(Collectors.toList());

        @NotNull FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
            }

            @Override
            public void onFailure(Throwable e) {
                TelemetrySubscriptionUpdate update;
                if (e instanceof UnauthorizedException) {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
                sendWsMsg(sessionRef, update);
            }
        };
        accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));
    }

    private void handleWsAttributesSubscription(@NotNull TelemetryWebSocketSessionRef sessionRef,
                                                @NotNull AttributesSubscriptionCmd cmd, String sessionId, @NotNull EntityId entityId) {
        @NotNull FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull List<AttributeKvEntry> data) {
                @NotNull List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), attributesData));

                @NotNull Map<String, Long> subState = new HashMap<>(attributesData.size());
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                @NotNull TbAttributeSubscriptionScope scope = StringUtils.isEmpty(cmd.getScope()) ? TbAttributeSubscriptionScope.ANY_SCOPE : TbAttributeSubscriptionScope.valueOf(cmd.getScope());

                TbAttributeSubscription sub = TbAttributeSubscription.builder()
                        .serviceId(serviceId)
                        .sessionId(sessionId)
                        .subscriptionId(cmd.getCmdId())
                        .tenantId(sessionRef.getSecurityCtx().getTenantId())
                        .entityId(entityId)
                        .allKeys(true)
                        .keyStates(subState)
                        .updateConsumer(DefaultTelemetryWebSocketService.this::sendWsMsg)
                        .scope(scope).build();
                oldSubService.addSubscription(sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                                                                                              FAILED_TO_FETCH_ATTRIBUTES);
                sendWsMsg(sessionRef, update);
            }
        };


        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, cmd.getScope(), callback));
        }
    }

    private void handleWsTimeseriesSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull TimeseriesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                @NotNull Optional<Set<String>> keysOptional = getKeys(cmd);

                if (keysOptional.isPresent()) {
                    handleWsTimeseriesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId);
                } else {
                    handleWsTimeseriesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsTimeseriesSubscriptionByKeys(@NotNull TelemetryWebSocketSessionRef sessionRef,
                                                      @NotNull TimeseriesSubscriptionCmd cmd, String sessionId, @NotNull EntityId entityId) {
        long startTs;
        if (cmd.getTimeWindow() > 0) {
            @NotNull List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            log.debug("[{}] fetching timeseries data for last {} ms for keys: ({}) for device : {}", sessionId, cmd.getTimeWindow(), cmd.getKeys(), entityId);
            startTs = cmd.getStartTs();
            long endTs = cmd.getStartTs() + cmd.getTimeWindow();
            @NotNull List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, cmd.getInterval(),
                                                                                                  getLimit(cmd.getLimit()), getAggregation(cmd.getAgg()))).collect(Collectors.toList());

            @NotNull final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));
        } else {
            @NotNull List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            startTs = System.currentTimeMillis();
            log.debug("[{}] fetching latest timeseries data for keys: ({}) for device : {}", sessionId, cmd.getKeys(), entityId);
            @NotNull final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findLatest(sessionRef.getSecurityCtx().getTenantId(), entityId, keys), callback, executor), callback::onFailure));
        }
    }

    private void handleWsTimeseriesSubscription(@NotNull TelemetryWebSocketSessionRef sessionRef,
                                                @NotNull TimeseriesSubscriptionCmd cmd, String sessionId, @NotNull EntityId entityId) {
        @NotNull FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(@NotNull List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
                @NotNull Map<String, Long> subState = new HashMap<>(data.size());
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));

                TbTimeseriesSubscription sub = TbTimeseriesSubscription.builder()
                                                                       .serviceId(serviceId)
                                                                       .sessionId(sessionId)
                                                                       .subscriptionId(cmd.getCmdId())
                                                                       .tenantId(sessionRef.getSecurityCtx().getTenantId())
                                                                       .entityId(entityId)
                                                                       .updateConsumer(DefaultTelemetryWebSocketService.this::sendWsMsg)
                                                                       .allKeys(true)
                                                                       .keyStates(subState).build();
                oldSubService.addSubscription(sub);
            }

            @Override
            public void onFailure(Throwable e) {
                TelemetrySubscriptionUpdate update;
                if (e instanceof UnauthorizedException) {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
                sendWsMsg(sessionRef, update);
            }
        };
        accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                on(r -> Futures.addCallback(tsService.findAllLatest(sessionRef.getSecurityCtx().getTenantId(), entityId), callback, executor), callback::onFailure));
    }

    @NotNull
    private FutureCallback<List<TsKvEntry>> getSubscriptionCallback(@NotNull final TelemetryWebSocketSessionRef sessionRef, @NotNull final TimeseriesSubscriptionCmd cmd, final String sessionId, final EntityId entityId, final long startTs, @NotNull final List<String> keys) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
                @NotNull Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, startTs));
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));

                TbTimeseriesSubscription sub = TbTimeseriesSubscription.builder()
                        .serviceId(serviceId)
                        .sessionId(sessionId)
                        .subscriptionId(cmd.getCmdId())
                        .tenantId(sessionRef.getSecurityCtx().getTenantId())
                        .entityId(entityId)
                        .updateConsumer(DefaultTelemetryWebSocketService.this::sendWsMsg)
                        .allKeys(false)
                        .keyStates(subState).build();
                oldSubService.addSubscription(sub);
            }

            @Override
            public void onFailure(Throwable e) {
                if (e instanceof TenantRateLimitException || e.getCause() instanceof TenantRateLimitException) {
                    log.trace("[{}] Tenant rate limit detected for subscription: [{}]:{}", sessionRef.getSecurityCtx().getTenantId(), entityId, cmd);
                } else {
                    log.info(FAILED_TO_FETCH_DATA, e);
                }
                @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                                                                                              FAILED_TO_FETCH_DATA);
                sendWsMsg(sessionRef, update);
            }
        };
    }

    private void unsubscribe(TelemetryWebSocketSessionRef sessionRef, @NotNull SubscriptionCmd cmd, String sessionId) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            oldSubService.cancelAllSessionSubscriptions(sessionId);
        } else {
            oldSubService.cancelSubscription(sessionId, cmd.getCmdId());
        }
    }

    private boolean validateSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityDataCmd cmd) {
        if (cmd.getCmdId() < 0) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null && !cmd.hasAnyCmd()) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityCountCmd cmd) {
        if (cmd.getCmdId() < 0) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST, "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull AlarmDataCmd cmd) {
        if (cmd.getCmdId() < 0) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull SubscriptionCmd cmd) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                                                                                          "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSessionMetadata(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull SubscriptionCmd cmd, String sessionId) {
        return validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId);
    }

    private boolean validateSessionMetadata(@NotNull TelemetryWebSocketSessionRef sessionRef, int cmdId, String sessionId) {
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            @NotNull TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmdId, SubscriptionErrorCode.INTERNAL_ERROR,
                                                                                          SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return false;
        } else {
            return true;
        }
    }

    private void sendWsMsg(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull EntityDataUpdate update) {
        sendWsMsg(sessionRef, update.getCmdId(), update);
    }

    private void sendWsMsg(@NotNull TelemetryWebSocketSessionRef sessionRef, @NotNull TelemetrySubscriptionUpdate update) {
        sendWsMsg(sessionRef, update.getSubscriptionId(), update);
    }

    private void sendWsMsg(@NotNull TelemetryWebSocketSessionRef sessionRef, int cmdId, Object update) {
        try {
            String msg = jsonMapper.writeValueAsString(update);
            executor.submit(() -> {
                try {
                    msgEndpoint.send(sessionRef, cmdId, msg);
                } catch (IOException e) {
                    log.warn("[{}] Failed to send reply: {}", sessionRef.getSessionId(), update, e);
                }
            });
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to encode reply: {}", sessionRef.getSessionId(), update, e);
        }
    }

    private void sendPing() {
        long currentTime = System.currentTimeMillis();
        wsSessionsMap.values().forEach(md ->
                executor.submit(() -> {
                    try {
                        msgEndpoint.sendPing(md.getSessionRef(), currentTime);
                    } catch (IOException e) {
                        log.warn("[{}] Failed to send ping: {}", md.getSessionRef().getSessionId(), e);
                    }
                }));
    }

    @NotNull
    private static Optional<Set<String>> getKeys(@NotNull TelemetryPluginCmd cmd) {
        if (!StringUtils.isEmpty(cmd.getKeys())) {
            @NotNull Set<String> keys = new HashSet<>();
            Collections.addAll(keys, cmd.getKeys().split(","));
            return Optional.of(keys);
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(@NotNull List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    @NotNull List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    @NotNull
    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final List<String> keys, @NotNull final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                @NotNull List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.find(tenantId, entityId, scope, keys));
                }

                @NotNull ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    @NotNull
    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, final List<String> keys, @NotNull final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.find(tenantId, entityId, scope, keys), callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    @NotNull
    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, @NotNull final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                @NotNull List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.findAll(tenantId, entityId, scope));
                }

                @NotNull ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    @NotNull
    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, @NotNull final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.findAll(tenantId, entityId, scope), callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    @NotNull
    private FutureCallback<ValidationResult> on(@NotNull Consumer<Void> success, @NotNull Consumer<Throwable> failure) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                ValidationResultCode resultCode = result.getResultCode();
                if (resultCode == ValidationResultCode.OK) {
                    success.accept(null);
                } else {
                    onFailure(ValidationCallback.getException(result));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                failure.accept(t);
            }
        };
    }


    @NotNull
    public static Aggregation getAggregation(String agg) {
        return StringUtils.isEmpty(agg) ? DEFAULT_AGGREGATION : Aggregation.valueOf(agg);
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

    @org.jetbrains.annotations.Nullable
    private DefaultTenantProfileConfiguration getTenantProfileConfiguration(@NotNull TelemetryWebSocketSessionRef sessionRef) {
        return Optional.ofNullable(tenantProfileCache.get(sessionRef.getSecurityCtx().getTenantId()))
                       .map(TenantProfile::getDefaultProfileConfiguration).orElse(null);
    }

}
