package org.echoiot.rule.engine.profile;

import lombok.AccessLevel;
import lombok.Getter;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.query.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


class ProfileState {

    private DeviceProfile deviceProfile;
    @Getter(AccessLevel.PACKAGE)
    private final List<DeviceProfileAlarm> alarmSettings = new CopyOnWriteArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<AlarmConditionFilterKey> entityKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<AlarmSeverity, Set<AlarmConditionFilterKey>>> alarmCreateKeys = new HashMap<>();
    private final Map<String, Set<AlarmConditionFilterKey>> alarmClearKeys = new HashMap<>();

    ProfileState(@NotNull DeviceProfile deviceProfile) {
        updateDeviceProfile(deviceProfile);
    }

    void updateDeviceProfile(@NotNull DeviceProfile deviceProfile) {
        this.deviceProfile = deviceProfile;
        alarmSettings.clear();
        alarmCreateKeys.clear();
        alarmClearKeys.clear();
        entityKeys.clear();
        if (deviceProfile.getProfileData().getAlarms() != null) {
            alarmSettings.addAll(deviceProfile.getProfileData().getAlarms());
            for (@NotNull DeviceProfileAlarm alarm : deviceProfile.getProfileData().getAlarms()) {
                @NotNull Map<AlarmSeverity, Set<AlarmConditionFilterKey>> createAlarmKeys = alarmCreateKeys.computeIfAbsent(alarm.getId(), id -> new HashMap<>());
                alarm.getCreateRules().forEach(((severity, alarmRule) -> {
                    @NotNull var ruleKeys = createAlarmKeys.computeIfAbsent(severity, id -> new HashSet<>());
                    for (@NotNull var keyFilter : alarmRule.getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        ruleKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, ruleKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarmRule);
                    AlarmSchedule schedule = alarmRule.getSchedule();
                    if (schedule != null) {
                        addScheduleDynamicValues(schedule);
                    }
                }));
                if (alarm.getClearRule() != null) {
                    @NotNull var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarm.getId(), id -> new HashSet<>());
                    for (@NotNull var keyFilter : alarm.getClearRule().getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        clearAlarmKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, clearAlarmKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarm.getClearRule());
                }
            }
        }
    }

    private void addScheduleDynamicValues(@NotNull AlarmSchedule schedule) {
        DynamicValue<String> dynamicValue = schedule.getDynamicValue();
        if (dynamicValue != null) {
            entityKeys.add(
                    new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                            dynamicValue.getSourceAttribute())
            );
        }
    }

    private void addEntityKeysFromAlarmConditionSpec(@NotNull AlarmRule alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            return;
        }
        AlarmConditionSpecType specType = spec.getType();
        switch (specType) {
            case DURATION:
                @NotNull DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
                if(duration.getPredicate().getDynamicValue() != null
                        && duration.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    duration.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
            case REPEATING:
                @NotNull RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;
                if(repeating.getPredicate().getDynamicValue() != null
                        && repeating.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    repeating.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
        }

    }

    private void addDynamicValuesRecursively(@NotNull KeyFilterPredicate predicate, @NotNull Set<AlarmConditionFilterKey> entityKeys, @NotNull Set<AlarmConditionFilterKey> ruleKeys) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                DynamicValue value = ((SimpleKeyFilterPredicate) predicate).getValue().getDynamicValue();
                if (value != null && (value.getSourceType() == DynamicValueSourceType.CURRENT_TENANT ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_CUSTOMER ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_DEVICE)) {
                    @NotNull AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, value.getSourceAttribute());
                    entityKeys.add(entityKey);
                    ruleKeys.add(entityKey);
                }
                break;
            case COMPLEX:
                for (@NotNull KeyFilterPredicate child : ((ComplexFilterPredicate) predicate).getPredicates()) {
                    addDynamicValuesRecursively(child, entityKeys, ruleKeys);
                }
                break;
        }
    }

    DeviceProfileId getProfileId() {
        return deviceProfile.getId();
    }

    @NotNull
    Set<AlarmConditionFilterKey> getCreateAlarmKeys(String id, AlarmSeverity severity) {
        Map<AlarmSeverity, Set<AlarmConditionFilterKey>> sKeys = alarmCreateKeys.get(id);
        if (sKeys == null) {
            return Collections.emptySet();
        } else {
            Set<AlarmConditionFilterKey> keys = sKeys.get(severity);
            if (keys == null) {
                return Collections.emptySet();
            } else {
                return keys;
            }
        }
    }

    @NotNull
    Set<AlarmConditionFilterKey> getClearAlarmKeys(String id) {
        Set<AlarmConditionFilterKey> keys = alarmClearKeys.get(id);
        if (keys == null) {
            return Collections.emptySet();
        } else {
            return keys;
        }
    }
}
