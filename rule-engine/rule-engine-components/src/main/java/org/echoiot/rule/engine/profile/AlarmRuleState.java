package org.echoiot.rule.engine.profile;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.profile.state.PersistedAlarmRuleState;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.common.msg.tools.SchedulerUtils;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Data
@Slf4j
class AlarmRuleState {

    private final AlarmSeverity severity;
    private final AlarmRule alarmRule;
    @NotNull
    private final AlarmConditionSpec spec;
    private final Set<AlarmConditionFilterKey> entityKeys;
    private PersistedAlarmRuleState state;
    private boolean updateFlag;
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    AlarmRuleState(AlarmSeverity severity, @NotNull AlarmRule alarmRule, Set<AlarmConditionFilterKey> entityKeys, @Nullable PersistedAlarmRuleState state, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        this.entityKeys = entityKeys;
        if (state != null) {
            this.state = state;
        } else {
            this.state = new PersistedAlarmRuleState(0L, 0L, 0L);
        }
        this.spec = getSpec(alarmRule);
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
    }

    public boolean validateTsUpdate(@NotNull Set<AlarmConditionFilterKey> changedKeys) {
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean validateAttrUpdate(@NotNull Set<AlarmConditionFilterKey> changedKeys) {
        //If the attribute was updated, but no new telemetry arrived - we ignore this until new telemetry is there.
        for (@NotNull AlarmConditionFilterKey key : entityKeys) {
            if (key.getType().equals(AlarmConditionKeyType.TIME_SERIES)) {
                return false;
            }
        }
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public AlarmConditionSpec getSpec(@NotNull AlarmRule alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            spec = new SimpleAlarmConditionSpec();
        }
        return spec;
    }

    public boolean checkUpdate() {
        if (updateFlag) {
            updateFlag = false;
            return true;
        } else {
            return false;
        }
    }

    public AlarmEvalResult eval(@NotNull DataSnapshot data) {
        boolean active = isActive(data, data.getTs());
        switch (spec.getType()) {
            case SIMPLE:
                return (active && eval(alarmRule.getCondition(), data)) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
            case DURATION:
                return evalDuration(data, active);
            case REPEATING:
                return evalRepeating(data, active);
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    private boolean isActive(@NotNull DataSnapshot data, long eventTs) {
        if (eventTs == 0L) {
            eventTs = System.currentTimeMillis();
        }
        if (alarmRule.getSchedule() == null) {
            return true;
        }
        switch (alarmRule.getSchedule().getType()) {
            case ANY_TIME:
                return true;
            case SPECIFIC_TIME:
                return isActiveSpecific((SpecificTimeSchedule) getSchedule(data, alarmRule), eventTs);
            case CUSTOM:
                return isActiveCustom((CustomTimeSchedule) getSchedule(data, alarmRule), eventTs);
            default:
                throw new RuntimeException("Unsupported schedule type: " + alarmRule.getSchedule().getType());
        }
    }

    private AlarmSchedule getSchedule(@NotNull DataSnapshot data, @NotNull AlarmRule alarmRule) {
        AlarmSchedule schedule = alarmRule.getSchedule();
        @Nullable EntityKeyValue dynamicValue = getDynamicPredicateValue(data, schedule.getDynamicValue());

        if (dynamicValue != null) {
            try {
                return JsonConverter.parse(dynamicValue.getJsonValue(), alarmRule.getSchedule().getClass());
            } catch (Exception e) {
                log.trace("Failed to parse AlarmSchedule from dynamicValue: {}", dynamicValue.getJsonValue(), e);
            }
        }
        return schedule;
    }

    private boolean isActiveSpecific(@NotNull SpecificTimeSchedule schedule, long eventTs) {
        @NotNull ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        @NotNull ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        if (schedule.getDaysOfWeek().size() != 7) {
            int dayOfWeek = zdt.getDayOfWeek().getValue();
            if (!schedule.getDaysOfWeek().contains(dayOfWeek)) {
                return false;
            }
        }
        long endsOn = schedule.getEndsOn();
        if (endsOn == 0) {
            // 24 hours in milliseconds
            endsOn = 86400000;
        }

        return isActive(eventTs, zoneId, zdt, schedule.getStartsOn(), endsOn);
    }

    private boolean isActiveCustom(@NotNull CustomTimeSchedule schedule, long eventTs) {
        @NotNull ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        @NotNull ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (@NotNull CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    long endsOn = item.getEndsOn();
                    if (endsOn == 0) {
                        // 24 hours in milliseconds
                        endsOn = 86400000;
                    }
                    return isActive(eventTs, zoneId, zdt, item.getStartsOn(), endsOn);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isActive(long eventTs, @NotNull ZoneId zoneId, @NotNull ZonedDateTime zdt, long startsOn, long endsOn) {
        long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long msFromStartOfDay = eventTs - startOfDay;
        if (startsOn <= endsOn) {
            return startsOn <= msFromStartOfDay && endsOn > msFromStartOfDay;
        } else {
            return startsOn < msFromStartOfDay || (0 < msFromStartOfDay && msFromStartOfDay < endsOn);
        }
    }

    public void clear() {
        if (state.getEventCount() > 0 || state.getLastEventTs() > 0 || state.getDuration() > 0) {
            state.setEventCount(0L);
            state.setLastEventTs(0L);
            state.setDuration(0L);
            updateFlag = true;
        }
    }

    @NotNull
    private AlarmEvalResult evalRepeating(@NotNull DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            state.setEventCount(state.getEventCount() + 1);
            updateFlag = true;
            long requiredRepeats = resolveRequiredRepeats(data);
            return state.getEventCount() >= requiredRepeats ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    @NotNull
    private AlarmEvalResult evalDuration(@NotNull DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            if (state.getLastEventTs() > 0) {
                if (data.getTs() > state.getLastEventTs()) {
                    state.setDuration(state.getDuration() + (data.getTs() - state.getLastEventTs()));
                    state.setLastEventTs(data.getTs());
                    updateFlag = true;
                }
            } else {
                state.setLastEventTs(data.getTs());
                state.setDuration(0L);
                updateFlag = true;
            }
            long requiredDurationInMs = resolveRequiredDurationInMs(data);
            return state.getDuration() > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private long resolveRequiredRepeats(@NotNull DataSnapshot data) {
        long repeatingTimes = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.REPEATING)) {
            RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;

            repeatingTimes = resolveDynamicValue(data, repeating.getPredicate());
        }
        return repeatingTimes;
    }

    private long resolveRequiredDurationInMs(@NotNull DataSnapshot data) {
        long durationTimeInMs = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.DURATION)) {
            DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
            TimeUnit timeUnit = duration.getUnit();

            durationTimeInMs = timeUnit.toMillis(resolveDynamicValue(data, duration.getPredicate()));
        }
        return durationTimeInMs;
    }

    @NotNull
    private Long resolveDynamicValue(@NotNull DataSnapshot data, @NotNull FilterPredicateValue<? extends Number> predicate) {
        DynamicValue<?> dynamicValue = predicate.getDynamicValue();
        @NotNull Long defaultValue = predicate.getDefaultValue().longValue();
        if (dynamicValue == null || dynamicValue.getSourceAttribute() == null) {
            return defaultValue;
        }

        @Nullable EntityKeyValue keyValue = getDynamicPredicateValue(data, dynamicValue);
        if (keyValue == null) {
            return defaultValue;
        }

        @Nullable var longValue = getLongValue(keyValue);
        if (longValue == null) {
            String sourceAttribute = dynamicValue.getSourceAttribute();
            throw new NumericParseException(String.format("Could not convert attribute '%s' with value '%s' to numeric value!", sourceAttribute, getStrValue(keyValue)));
        }
        return longValue;
    }

    @NotNull
    public AlarmEvalResult eval(long ts, @NotNull DataSnapshot dataSnapshot) {
        switch (spec.getType()) {
            case SIMPLE:
            case REPEATING:
                return AlarmEvalResult.NOT_YET_TRUE;
            case DURATION:
                long requiredDurationInMs = resolveRequiredDurationInMs(dataSnapshot);
                if (requiredDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    long duration = state.getDuration() + (ts - state.getLastEventTs());
                    if (isActive(dataSnapshot, ts)) {
                        return duration > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
                    } else {
                        return AlarmEvalResult.FALSE;
                    }
                }
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    private boolean eval(@NotNull AlarmCondition condition, @NotNull DataSnapshot data) {
        boolean eval = true;
        for (@NotNull var filter : condition.getCondition()) {
            @Nullable EntityKeyValue value;
            if (filter.getKey().getType().equals(AlarmConditionKeyType.CONSTANT)) {
                try {
                    value = getConstantValue(filter);
                } catch (RuntimeException e) {
                    log.warn("Failed to parse constant value from filter: {}", filter, e);
                    value = null;
                }
            } else {
                value = data.getValue(filter.getKey());
            }
            if (value == null) {
                return false;
            }
            eval = eval && eval(data, value, filter.getPredicate(), filter);
        }
        return eval;
    }

    @NotNull
    private EntityKeyValue getConstantValue(@NotNull AlarmConditionFilter filter) {
        @NotNull EntityKeyValue value = new EntityKeyValue();
        String valueStr = filter.getValue().toString();
        switch (filter.getValueType()) {
            case STRING:
                value.setStrValue(valueStr);
                break;
            case DATE_TIME:
                value.setLngValue(Long.valueOf(valueStr));
                break;
            case NUMERIC:
                value.setDblValue(Double.valueOf(valueStr));
                break;
            case BOOLEAN:
                value.setBoolValue(Boolean.valueOf(valueStr));
                break;
        }
        return value;
    }

    private boolean eval(@NotNull DataSnapshot data, @NotNull EntityKeyValue value, @NotNull KeyFilterPredicate predicate, @NotNull AlarmConditionFilter filter) {
        switch (predicate.getType()) {
            case STRING:
                return evalStrPredicate(data, value, (StringFilterPredicate) predicate, filter);
            case NUMERIC:
                return evalNumPredicate(data, value, (NumericFilterPredicate) predicate, filter);
            case BOOLEAN:
                return evalBoolPredicate(data, value, (BooleanFilterPredicate) predicate, filter);
            case COMPLEX:
                return evalComplexPredicate(data, value, (ComplexFilterPredicate) predicate, filter);
            default:
                return false;
        }
    }

    private boolean evalComplexPredicate(@NotNull DataSnapshot data, @NotNull EntityKeyValue ekv, @NotNull ComplexFilterPredicate predicate, @NotNull AlarmConditionFilter filter) {
        switch (predicate.getOperation()) {
            case OR:
                for (@NotNull KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (eval(data, ekv, kfp, filter)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (@NotNull KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (!eval(data, ekv, kfp, filter)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalBoolPredicate(@NotNull DataSnapshot data, @NotNull EntityKeyValue ekv, @NotNull BooleanFilterPredicate predicate, @NotNull AlarmConditionFilter filter) {
        @Nullable Boolean val = getBoolValue(ekv);
        if (val == null) {
            return false;
        }
        @Nullable Boolean predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getBoolValue);
        if (predicateValue == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case EQUAL:
                return val.equals(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalNumPredicate(@NotNull DataSnapshot data, @NotNull EntityKeyValue ekv, @NotNull NumericFilterPredicate predicate, @NotNull AlarmConditionFilter filter) {
        @Nullable Double val = getDblValue(ekv);
        if (val == null) {
            return false;
        }
        @Nullable Double predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getDblValue);
        if (predicateValue == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case GREATER:
                return val > predicateValue;
            case GREATER_OR_EQUAL:
                return val >= predicateValue;
            case LESS:
                return val < predicateValue;
            case LESS_OR_EQUAL:
                return val <= predicateValue;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalStrPredicate(@NotNull DataSnapshot data, @NotNull EntityKeyValue ekv, @NotNull StringFilterPredicate predicate, @NotNull AlarmConditionFilter filter) {
        @Nullable String val = getStrValue(ekv);
        if (val == null) {
            return false;
        }
        @Nullable String predicateValue = getPredicateValue(data, predicate.getValue(), filter, AlarmRuleState::getStrValue);
        if (predicateValue == null) {
            return false;
        }
        if (predicate.isIgnoreCase()) {
            val = val.toLowerCase();
            predicateValue = predicateValue.toLowerCase();
        }
        switch (predicate.getOperation()) {
            case CONTAINS:
                return val.contains(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case STARTS_WITH:
                return val.startsWith(predicateValue);
            case ENDS_WITH:
                return val.endsWith(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case NOT_CONTAINS:
                return !val.contains(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    @Nullable
    private <T> T getPredicateValue(@NotNull DataSnapshot data, @NotNull FilterPredicateValue<T> value, @NotNull AlarmConditionFilter filter, @NotNull Function<EntityKeyValue, T> transformFunction) {
        @Nullable EntityKeyValue ekv = getDynamicPredicateValue(data, value.getDynamicValue());
        if (ekv != null) {
            T result = transformFunction.apply(ekv);
            if (result != null) {
                return result;
            }
        }
        if (filter.getKey().getType() != AlarmConditionKeyType.CONSTANT) {
            return value.getDefaultValue();
        } else {
            return null;
        }
    }

    @Nullable
    private <T> EntityKeyValue getDynamicPredicateValue(@NotNull DataSnapshot data, @Nullable DynamicValue<T> value) {
        @Nullable EntityKeyValue ekv = null;
        if (value != null) {
            switch (value.getSourceType()) {
                case CURRENT_DEVICE:
                    ekv = data.getValue(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, value.getSourceAttribute()));
                    if (ekv != null || !value.isInherit()) {
                        break;
                    }
                case CURRENT_CUSTOMER:
                    ekv = dynamicPredicateValueCtx.getCustomerValue(value.getSourceAttribute());
                    if (ekv != null || !value.isInherit()) {
                        break;
                    }
                case CURRENT_TENANT:
                    ekv = dynamicPredicateValueCtx.getTenantValue(value.getSourceAttribute());
            }
        }
        return ekv;
    }

    @Nullable
    private static String getStrValue(@NotNull EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().toString() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().toString() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? ekv.getBoolValue().toString() : null;
            case STRING:
                return ekv.getStrValue();
            case JSON:
                return ekv.getJsonValue();
            default:
                return null;
        }
    }

    @Nullable
    private static Double getDblValue(@NotNull EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().doubleValue() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1.0 : 0.0) : null;
            case STRING:
                try {
                    return Double.parseDouble(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    @Nullable
    private static Boolean getBoolValue(@NotNull EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue() > 0 : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() > 0 : null;
            case BOOLEAN:
                return ekv.getBoolValue();
            case STRING:
                try {
                    return Boolean.parseBoolean(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    @Nullable
    private static Long getLongValue(@NotNull EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue();
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().longValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1 : 0L) : null;
            case STRING:
                try {
                    return Long.parseLong(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Long.parseLong(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }
}
