package org.echoiot.rule.engine.profile;

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.profile.state.PersistedAlarmState;
import org.echoiot.rule.engine.profile.state.PersistedDeviceState;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.device.profile.AlarmConditionFilterKey;
import org.echoiot.server.common.data.device.profile.AlarmConditionKeyType;
import org.echoiot.server.common.data.device.profile.DeviceProfileAlarm;
import org.echoiot.server.common.data.exception.ApiUsageLimitsExceededException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.KvEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.query.EntityKey;
import org.echoiot.server.common.data.query.EntityKeyType;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.dao.sql.query.EntityKeyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
class DeviceState {

    private final boolean persistState;
    private final DeviceId deviceId;
    private final ProfileState deviceProfile;
    private RuleNodeState state;
    @Nullable
    private PersistedDeviceState pds;
    private DataSnapshot latestValues;
    private final ConcurrentMap<String, AlarmState> alarmStates = new ConcurrentHashMap<>();
    @NotNull
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    DeviceState(@NotNull TbContext ctx, @NotNull TbDeviceProfileNodeConfiguration config, DeviceId deviceId, @NotNull ProfileState deviceProfile, @Nullable RuleNodeState state) {
        this.persistState = config.isPersistAlarmRulesState();
        this.deviceId = deviceId;
        this.deviceProfile = deviceProfile;

        this.dynamicPredicateValueCtx = new DynamicPredicateValueCtxImpl(ctx.getTenantId(), deviceId, ctx);

        if (config.isPersistAlarmRulesState()) {
            if (state != null) {
                this.state = state;
            } else {
                this.state = ctx.findRuleNodeStateForEntity(deviceId);
            }
            if (this.state != null) {
                pds = JacksonUtil.fromString(this.state.getStateData(), PersistedDeviceState.class);
            } else {
                this.state = new RuleNodeState();
                this.state.setRuleNodeId(ctx.getSelfId());
                this.state.setEntityId(deviceId);
                pds = new PersistedDeviceState();
                pds.setAlarmStates(new HashMap<>());
            }
        }
        if (pds != null) {
            for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    public void updateProfile(@NotNull TbContext ctx, @NotNull DeviceProfile deviceProfile) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> oldKeys = Set.copyOf(this.deviceProfile.getEntityKeys());
        this.deviceProfile.updateDeviceProfile(deviceProfile);
        if (latestValues != null) {
            @NotNull Set<AlarmConditionFilterKey> keysToFetch = new HashSet<>(this.deviceProfile.getEntityKeys());
            keysToFetch.removeAll(oldKeys);
            if (!keysToFetch.isEmpty()) {
                addEntityKeysToSnapshot(ctx, deviceId, keysToFetch, latestValues);
            }
        }
        @NotNull Set<String> newAlarmStateIds = this.deviceProfile.getAlarmSettings().stream().map(DeviceProfileAlarm::getId).collect(Collectors.toSet());
        alarmStates.keySet().removeIf(id -> !newAlarmStateIds.contains(id));
        for (@NotNull DeviceProfileAlarm alarm : this.deviceProfile.getAlarmSettings()) {
            if (alarmStates.containsKey(alarm.getId())) {
                alarmStates.get(alarm.getId()).updateState(alarm, getOrInitPersistedAlarmState(alarm));
            } else {
                alarmStates.putIfAbsent(alarm.getId(), new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    public void harvestAlarms(@NotNull TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        log.debug("[{}] Going to harvest alarms: {}", ctx.getSelfId(), ts);
        boolean stateChanged = false;
        for (@NotNull AlarmState state : alarmStates.values()) {
            stateChanged |= state.process(ctx, ts);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    public void process(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        if (latestValues == null) {
            latestValues = fetchLatestValues(ctx, deviceId);
        }
        boolean stateChanged = false;
        if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            stateChanged = processTelemetry(ctx, msg);
        } else if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
            stateChanged = processAttributesUpdateRequest(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ACTIVITY_EVENT) || msg.getType().equals(DataConstants.INACTIVITY_EVENT)) {
            stateChanged = processDeviceActivityEvent(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED)) {
            stateChanged = processAttributesUpdateNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
            stateChanged = processAttributesDeleteNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ALARM_CLEAR)) {
            stateChanged = processAlarmClearNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ALARM_ACK)) {
            processAlarmAckNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ALARM_DELETE)) {
            processAlarmDeleteNotification(ctx, msg);
        } else {
            if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED) || msg.getType().equals(DataConstants.ENTITY_UNASSIGNED)) {
                dynamicPredicateValueCtx.resetCustomer();
            }
            ctx.tellSuccess(msg);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    private boolean processDeviceActivityEvent(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            return processTelemetry(ctx, msg);
        } else {
            return processAttributes(ctx, msg, scope);
        }
    }

    private boolean processAlarmClearNotification(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        boolean stateChanged = false;
        @Nullable Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            @NotNull AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            stateChanged |= alarmState.processAlarmClear(ctx, alarmNf);
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    private void processAlarmAckNotification(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        @Nullable Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            @NotNull AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            alarmState.processAckAlarm(alarmNf);
        }
        ctx.tellSuccess(msg);
    }

    private void processAlarmDeleteNotification(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        @Nullable Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        alarmStates.values().removeIf(alarmState -> alarmState.getCurrentAlarm() != null
                && alarmState.getCurrentAlarm().getId().equals(alarm.getId()));
        ctx.tellSuccess(msg);
    }

    private boolean processAttributesUpdateNotification(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        return processAttributes(ctx, msg, scope);
    }

    private boolean processAttributesDeleteNotification(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        @NotNull List<String> keys = new ArrayList<>();
        new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray().forEach(e -> keys.add(e.getAsString()));
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        if (!keys.isEmpty()) {
            @NotNull EntityKeyType keyType = getKeyTypeFromScope(scope);
            @NotNull Set<AlarmConditionFilterKey> removedKeys = keys.stream().map(key -> new EntityKey(keyType, key))
                                                                    .peek(latestValues::removeValue)
                                                                    .map(DataSnapshot::toConditionKey).collect(Collectors.toSet());
            @NotNull SnapshotUpdate update = new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, removedKeys);

            for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                @NotNull AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    protected boolean processAttributesUpdateRequest(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        return processAttributes(ctx, msg, DataConstants.CLIENT_SCOPE);
    }

    private boolean processAttributes(@NotNull TbContext ctx, @NotNull TbMsg msg, String scope) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        @NotNull Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
        if (!attributes.isEmpty()) {
            @NotNull SnapshotUpdate update = merge(latestValues, attributes, scope);
            for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                @NotNull AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    protected boolean processTelemetry(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        @NotNull Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(new JsonParser().parse(msg.getData()), msg.getMetaDataTs());
        // iterate over data by ts (ASC order).
        for (@NotNull Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            @NotNull SnapshotUpdate update = merge(latestValues, ts, data);
            if (update.hasUpdate()) {
                for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                    @NotNull AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                            a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                    try {
                        stateChanged |= alarmState.process(ctx, msg, latestValues, update);
                    } catch (ApiUsageLimitsExceededException e) {
                        alarmStates.remove(alarm.getId());
                        throw e;
                    }
                }
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    @NotNull
    private SnapshotUpdate merge(@NotNull DataSnapshot latestValues, Long newTs, @NotNull List<KvEntry> data) {
        @NotNull Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (@NotNull KvEntry entry : data) {
            @NotNull AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.TIME_SERIES, keys);
    }

    @NotNull
    private SnapshotUpdate merge(@NotNull DataSnapshot latestValues, @NotNull Set<AttributeKvEntry> attributes, String scope) {
        long newTs = 0;
        @NotNull Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (@NotNull AttributeKvEntry entry : attributes) {
            newTs = Math.max(newTs, entry.getLastUpdateTs());
            @NotNull AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, keys);
    }

    @NotNull
    private static EntityKeyType getKeyTypeFromScope(@NotNull String scope) {
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                return EntityKeyType.CLIENT_ATTRIBUTE;
            case DataConstants.SHARED_SCOPE:
                return EntityKeyType.SHARED_ATTRIBUTE;
            case DataConstants.SERVER_SCOPE:
                return EntityKeyType.SERVER_ATTRIBUTE;
        }
        return EntityKeyType.ATTRIBUTE;
    }

    @NotNull
    private DataSnapshot fetchLatestValues(@NotNull TbContext ctx, @NotNull EntityId originator) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> entityKeysToFetch = deviceProfile.getEntityKeys();
        @NotNull DataSnapshot result = new DataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    private void addEntityKeysToSnapshot(@NotNull TbContext ctx, @NotNull EntityId originator, @NotNull Set<AlarmConditionFilterKey> entityKeysToFetch, @NotNull DataSnapshot result) throws InterruptedException, ExecutionException {
        @NotNull Set<String> attributeKeys = new HashSet<>();
        @NotNull Set<String> latestTsKeys = new HashSet<>();

        @Nullable Device device = null;
        for (@NotNull AlarmConditionFilterKey entityKey : entityKeysToFetch) {
            String key = entityKey.getKey();
            switch (entityKey.getType()) {
                case ATTRIBUTE:
                    attributeKeys.add(key);
                    break;
                case TIME_SERIES:
                    latestTsKeys.add(key);
                    break;
                case ENTITY_FIELD:
                    if (device == null) {
                        device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(originator.getId()));
                    }
                    if (device != null) {
                        switch (key) {
                            case EntityKeyMapping.NAME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getName()));
                                break;
                            case EntityKeyMapping.TYPE:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getType()));
                                break;
                            case EntityKeyMapping.CREATED_TIME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromLong(device.getCreatedTime()));
                                break;
                            case EntityKeyMapping.LABEL:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getLabel()));
                                break;
                        }
                    }
                    break;
            }
        }

        if (!latestTsKeys.isEmpty()) {
            List<TsKvEntry> data = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), originator, latestTsKeys).get();
            for (@NotNull TsKvEntry entry : data) {
                if (entry.getValue() != null) {
                    result.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey()), entry.getTs(), toEntityValue(entry));
                }
            }
        }
        if (!attributeKeys.isEmpty()) {
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.CLIENT_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SHARED_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SERVER_SCOPE, attributeKeys).get());
        }
    }

    private void addToSnapshot(@NotNull DataSnapshot snapshot, @NotNull List<AttributeKvEntry> data) {
        for (@NotNull AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                @NotNull EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey()), entry.getLastUpdateTs(), value);
            }
        }
    }

    @NotNull
    public static EntityKeyValue toEntityValue(@NotNull KvEntry entry) {
        switch (entry.getDataType()) {
            case STRING:
                return EntityKeyValue.fromString(entry.getStrValue().get());
            case LONG:
                return EntityKeyValue.fromLong(entry.getLongValue().get());
            case DOUBLE:
                return EntityKeyValue.fromDouble(entry.getDoubleValue().get());
            case BOOLEAN:
                return EntityKeyValue.fromBool(entry.getBooleanValue().get());
            case JSON:
                return EntityKeyValue.fromJson(entry.getJsonValue().get());
            default:
                throw new RuntimeException("Can't parse entry: " + entry.getDataType());
        }
    }

    public DeviceProfileId getProfileId() {
        return deviceProfile.getProfileId();
    }

    @Nullable
    private PersistedAlarmState getOrInitPersistedAlarmState(@NotNull DeviceProfileAlarm alarm) {
        if (pds != null) {
            PersistedAlarmState alarmState = pds.getAlarmStates().get(alarm.getId());
            if (alarmState == null) {
                alarmState = new PersistedAlarmState();
                alarmState.setCreateRuleStates(new HashMap<>());
                pds.getAlarmStates().put(alarm.getId(), alarmState);
            }
            return alarmState;
        } else {
            return null;
        }
    }

}
