/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.TenantRateLimitException;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.ValidationCallback;
import org.thingsboard.server.service.security.ValidationResult;
import org.thingsboard.server.service.security.ValidationResultCode;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.subscription.TbAttributeSubscription;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;
import org.thingsboard.server.service.subscription.TbEntityDataSubscriptionService;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbTimeseriesSubscription;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.SubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.TelemetryPluginCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.UnsubscribeCmd;
import org.thingsboard.server.service.telemetry.exception.UnauthorizedException;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.TelemetrySubscriptionUpdate;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 27.03.18.
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

    @Autowired
    private TbLocalSubscriptionService oldSubService;

    @Autowired
    private TbEntityDataSubscriptionService entityDataSubService;

    @Autowired
    private TelemetryWebSocketMsgEndpoint msgEndpoint;

    @Autowired
    private AccessValidator accessValidator;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    @Value("${server.ws.ping_timeout:30000}")
    private long pingTimeout;

    private ConcurrentMap<TenantId, Set<String>> tenantSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<CustomerId, Set<String>> customerSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> regularUserSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> publicUserSubscriptionsMap = new ConcurrentHashMap<>();

    private ExecutorService executor;
    private String serviceId;

    private ScheduledExecutorService pingExecutor;

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        executor = ThingsBoardExecutors.newWorkStealingPool(50, getClass());

        pingExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("telemetry-web-socket-ping"));
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
    public void handleWebSocketSessionEvent(TelemetryWebSocketSessionRef sessionRef, SessionEvent event) {
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
    public void handleWebSocketMsg(TelemetryWebSocketSessionRef sessionRef, String msg) {
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

    private void handleWsEntityDataCmd(TelemetryWebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsEntityCountCmd(TelemetryWebSocketSessionRef sessionRef, EntityCountCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsAlarmDataCmd(TelemetryWebSocketSessionRef sessionRef, AlarmDataCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)
                && validateSubscriptionCmd(sessionRef, cmd)) {
            entityDataSubService.handleCmd(sessionRef, cmd);
        }
    }

    private void handleWsDataUnsubscribeCmd(TelemetryWebSocketSessionRef sessionRef, UnsubscribeCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId)) {
            entityDataSubService.cancelSubscription(sessionRef.getSessionId(), cmd);
        }
    }

    @Override
    public void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate update) {
        sendWsMsg(sessionId, update.getSubscriptionId(), update);
    }

    @Override
    public void sendWsMsg(String sessionId, CmdUpdate update) {
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

    private void processSessionClose(TelemetryWebSocketSessionRef sessionRef) {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration != null) {
            String sessionId = "[" + sessionRef.getSessionId() + "]";

            if (tenantProfileConfiguration.getMaxWsSubscriptionsPerTenant() > 0) {
                Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (tenantSubscriptions) {
                    tenantSubscriptions.removeIf(subId -> subId.startsWith(sessionId));
                }
            }
            if (sessionRef.getSecurityCtx().isCustomerUser()) {
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerCustomer() > 0) {
                    Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (customerSessions) {
                        customerSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerRegularUser() > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (regularUserSessions) {
                        regularUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
                if (tenantProfileConfiguration.getMaxWsSubscriptionsPerPublicUser() > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (publicUserSessions) {
                        publicUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                    }
                }
            }
        }
    }

    private boolean processSubscription(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd) {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) return true;

        String subId = "[" + sessionRef.getSessionId() + "]:[" + cmd.getCmdId() + "]";
        try {
            if (tenantProfileConfiguration.getMaxWsSubscriptionsPerTenant() > 0) {
                Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
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
                    Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
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
                    Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
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
                    Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
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

    private void handleWsAttributesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, AttributesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                log.debug("[{}] fetching latest attributes ({}) values for device: {}", sessionId, cmd.getKeys(), entityId);
                Optional<Set<String>> keysOptional = getKeys(cmd);
                if (keysOptional.isPresent()) {
                    List<String> keys = new ArrayList<>(keysOptional.get());
                    handleWsAttributesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId, keys);
                } else {
                    handleWsAttributesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsAttributesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId,
                                                      List<String> keys) {
        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, 0L));
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                TbAttributeSubscriptionScope scope = StringUtils.isEmpty(cmd.getScope()) ? TbAttributeSubscriptionScope.ANY_SCOPE : TbAttributeSubscriptionScope.valueOf(cmd.getScope());

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

    private void handleWsHistoryCmd(TelemetryWebSocketSessionRef sessionRef, GetHistoryCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty() || cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getKeys() == null || cmd.getKeys().isEmpty()) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Keys are empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
        List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
        List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), getAggregation(cmd.getAgg())))
                .collect(Collectors.toList());

        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
            }

            @Override
            public void onFailure(Throwable e) {
                TelemetrySubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
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

    private void handleWsAttributesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(attributesData.size());
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                TbAttributeSubscriptionScope scope = StringUtils.isEmpty(cmd.getScope()) ? TbAttributeSubscriptionScope.ANY_SCOPE : TbAttributeSubscriptionScope.valueOf(cmd.getScope());

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
                TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
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

    private void handleWsTimeseriesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, TimeseriesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                Optional<Set<String>> keysOptional = getKeys(cmd);

                if (keysOptional.isPresent()) {
                    handleWsTimeseriesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId);
                } else {
                    handleWsTimeseriesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsTimeseriesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        long startTs;
        if (cmd.getTimeWindow() > 0) {
            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            log.debug("[{}] fetching timeseries data for last {} ms for keys: ({}) for device : {}", sessionId, cmd.getTimeWindow(), cmd.getKeys(), entityId);
            startTs = cmd.getStartTs();
            long endTs = cmd.getStartTs() + cmd.getTimeWindow();
            List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, cmd.getInterval(),
                    getLimit(cmd.getLimit()), getAggregation(cmd.getAgg()))).collect(Collectors.toList());

            final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));
        } else {
            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            startTs = System.currentTimeMillis();
            log.debug("[{}] fetching latest timeseries data for keys: ({}) for device : {}", sessionId, cmd.getKeys(), entityId);
            final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findLatest(sessionRef.getSecurityCtx().getTenantId(), entityId, keys), callback, executor), callback::onFailure));
        }
    }

    private void handleWsTimeseriesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
                Map<String, Long> subState = new HashMap<>(data.size());
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
                if (UnauthorizedException.class.isInstance(e)) {
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

    private FutureCallback<List<TsKvEntry>> getSubscriptionCallback(final TelemetryWebSocketSessionRef sessionRef, final TimeseriesSubscriptionCmd cmd, final String sessionId, final EntityId entityId, final long startTs, final List<String> keys) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new TelemetrySubscriptionUpdate(cmd.getCmdId(), data));
                Map<String, Long> subState = new HashMap<>(keys.size());
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
                TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_DATA);
                sendWsMsg(sessionRef, update);
            }
        };
    }

    private void unsubscribe(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            oldSubService.cancelAllSessionSubscriptions(sessionId);
        } else {
            oldSubService.cancelSubscription(sessionId, cmd.getCmdId());
        }
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        if (cmd.getCmdId() < 0) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null && !cmd.hasAnyCmd()) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, EntityCountCmd cmd) {
        if (cmd.getCmdId() < 0) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST, "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, AlarmDataCmd cmd) {
        if (cmd.getCmdId() < 0) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Cmd id is negative value!");
            sendWsMsg(sessionRef, update);
            return false;
        } else if (cmd.getQuery() == null) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Query is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSessionMetadata(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        return validateSessionMetadata(sessionRef, cmd.getCmdId(), sessionId);
    }

    private boolean validateSessionMetadata(TelemetryWebSocketSessionRef sessionRef, int cmdId, String sessionId) {
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(cmdId, SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return false;
        } else {
            return true;
        }
    }

    private void sendWsMsg(TelemetryWebSocketSessionRef sessionRef, EntityDataUpdate update) {
        sendWsMsg(sessionRef, update.getCmdId(), update);
    }

    private void sendWsMsg(TelemetryWebSocketSessionRef sessionRef, TelemetrySubscriptionUpdate update) {
        sendWsMsg(sessionRef, update.getSubscriptionId(), update);
    }

    private void sendWsMsg(TelemetryWebSocketSessionRef sessionRef, int cmdId, Object update) {
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

    private static Optional<Set<String>> getKeys(TelemetryPluginCmd cmd) {
        if (!StringUtils.isEmpty(cmd.getKeys())) {
            Set<String> keys = new HashSet<>();
            Collections.addAll(keys, cmd.getKeys().split(","));
            return Optional.of(keys);
        } else {
            return Optional.empty();
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.find(tenantId, entityId, scope, keys));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
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

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.findAll(tenantId, entityId, scope));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, final FutureCallback<List<AttributeKvEntry>> callback) {
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

    private FutureCallback<ValidationResult> on(Consumer<Void> success, Consumer<Throwable> failure) {
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


    public static Aggregation getAggregation(String agg) {
        return StringUtils.isEmpty(agg) ? DEFAULT_AGGREGATION : Aggregation.valueOf(agg);
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

    private DefaultTenantProfileConfiguration getTenantProfileConfiguration(TelemetryWebSocketSessionRef sessionRef) {
        return Optional.ofNullable(tenantProfileCache.get(sessionRef.getSecurityCtx().getTenantId()))
                .map(TenantProfile::getDefaultProfileConfiguration).orElse(null);
    }

}
