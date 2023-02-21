package org.echoiot.server.service.subscription;

import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.echoiot.server.service.telemetry.sub.SubscriptionErrorCode;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.BaseAttributeKvEntry;
import org.echoiot.server.common.data.kv.BasicTsKvEntry;
import org.echoiot.server.common.data.kv.BooleanDataEntry;
import org.echoiot.server.common.data.kv.DataType;
import org.echoiot.server.common.data.kv.DoubleDataEntry;
import org.echoiot.server.common.data.kv.JsonDataEntry;
import org.echoiot.server.common.data.kv.KvEntry;
import org.echoiot.server.common.data.kv.LongDataEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.KeyValueProto;
import org.echoiot.server.gen.transport.TransportProtos.KeyValueType;
import org.echoiot.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.echoiot.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.echoiot.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.echoiot.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.echoiot.server.gen.transport.TransportProtos.TbAttributeSubscriptionProto;
import org.echoiot.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.echoiot.server.gen.transport.TransportProtos.TbSubscriptionCloseProto;
import org.echoiot.server.gen.transport.TransportProtos.TbSubscriptionKetStateProto;
import org.echoiot.server.gen.transport.TransportProtos.TbSubscriptionProto;
import org.echoiot.server.gen.transport.TransportProtos.TbSubscriptionUpdateProto;
import org.echoiot.server.gen.transport.TransportProtos.TbSubscriptionUpdateTsValue;
import org.echoiot.server.gen.transport.TransportProtos.TbTimeSeriesDeleteProto;
import org.echoiot.server.gen.transport.TransportProtos.TbTimeSeriesSubscriptionProto;
import org.echoiot.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreMsg;
import org.echoiot.server.gen.transport.TransportProtos.TsKvProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TbSubscriptionUtils {

    public static ToCoreMsg toNewSubscriptionProto(@NotNull TbSubscription subscription) {
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        TbSubscriptionProto subscriptionProto = TbSubscriptionProto.newBuilder()
                .setServiceId(subscription.getServiceId())
                .setSessionId(subscription.getSessionId())
                .setSubscriptionId(subscription.getSubscriptionId())
                .setTenantIdMSB(subscription.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(subscription.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(subscription.getEntityId().getEntityType().name())
                .setEntityIdMSB(subscription.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(subscription.getEntityId().getId().getLeastSignificantBits()).build();

        switch (subscription.getType()) {
            case TIMESERIES:
                @NotNull TbTimeseriesSubscription tSub = (TbTimeseriesSubscription) subscription;
                TbTimeSeriesSubscriptionProto.Builder tSubProto = TbTimeSeriesSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setAllKeys(tSub.isAllKeys());
                tSub.getKeyStates().forEach((key, value) -> tSubProto.addKeyStates(
                        TbSubscriptionKetStateProto.newBuilder().setKey(key).setTs(value).build()));
                tSubProto.setStartTime(tSub.getStartTime());
                tSubProto.setEndTime(tSub.getEndTime());
                tSubProto.setLatestValues(tSub.isLatestValues());
                msgBuilder.setTelemetrySub(tSubProto.build());
                break;
            case ATTRIBUTES:
                @NotNull TbAttributeSubscription aSub = (TbAttributeSubscription) subscription;
                TbAttributeSubscriptionProto.Builder aSubProto = TbAttributeSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setAllKeys(aSub.isAllKeys())
                        .setScope(aSub.getScope().name());
                aSub.getKeyStates().forEach((key, value) -> aSubProto.addKeyStates(
                        TbSubscriptionKetStateProto.newBuilder().setKey(key).setTs(value).build()));
                msgBuilder.setAttributeSub(aSubProto.build());
                break;
            case ALARMS:
                @NotNull TbAlarmsSubscription alarmSub = (TbAlarmsSubscription) subscription;
                TransportProtos.TbAlarmSubscriptionProto.Builder alarmSubProto = TransportProtos.TbAlarmSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setTs(alarmSub.getTs());
                msgBuilder.setAlarmSub(alarmSubProto.build());
                break;
        }
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toCloseSubscriptionProto(@NotNull TbSubscription subscription) {
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        TbSubscriptionCloseProto closeProto = TbSubscriptionCloseProto.newBuilder()
                .setSessionId(subscription.getSessionId())
                .setSubscriptionId(subscription.getSubscriptionId()).build();
        msgBuilder.setSubClose(closeProto);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static TbSubscription fromProto(@NotNull TbAttributeSubscriptionProto attributeSub) {
        TbSubscriptionProto subProto = attributeSub.getSub();
        TbAttributeSubscription.TbAttributeSubscriptionBuilder builder = TbAttributeSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(TenantId.fromUUID(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));

        builder.scope(TbAttributeSubscriptionScope.valueOf(attributeSub.getScope()));
        builder.allKeys(attributeSub.getAllKeys());
        @NotNull Map<String, Long> keyStates = new HashMap<>();
        attributeSub.getKeyStatesList().forEach(ksProto -> keyStates.put(ksProto.getKey(), ksProto.getTs()));
        builder.keyStates(keyStates);
        return builder.build();
    }

    public static TbSubscription fromProto(@NotNull TbTimeSeriesSubscriptionProto telemetrySub) {
        TbSubscriptionProto subProto = telemetrySub.getSub();
        TbTimeseriesSubscription.TbTimeseriesSubscriptionBuilder builder = TbTimeseriesSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(TenantId.fromUUID(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));

        builder.allKeys(telemetrySub.getAllKeys());
        @NotNull Map<String, Long> keyStates = new HashMap<>();
        telemetrySub.getKeyStatesList().forEach(ksProto -> keyStates.put(ksProto.getKey(), ksProto.getTs()));
        builder.startTime(telemetrySub.getStartTime());
        builder.endTime(telemetrySub.getEndTime());
        builder.latestValues(telemetrySub.getLatestValues());
        builder.keyStates(keyStates);
        return builder.build();
    }

    public static TbSubscription fromProto(@NotNull TransportProtos.TbAlarmSubscriptionProto alarmSub) {
        TbSubscriptionProto subProto = alarmSub.getSub();
        TbAlarmsSubscription.TbAlarmsSubscriptionBuilder builder = TbAlarmsSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(TenantId.fromUUID(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));
        builder.ts(alarmSub.getTs());
        return builder.build();
    }

    @NotNull
    public static TelemetrySubscriptionUpdate fromProto(@NotNull TbSubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new TelemetrySubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            @NotNull Map<String, List<Object>> data = new TreeMap<>();
            proto.getDataList().forEach(v -> {
                @NotNull List<Object> values = data.computeIfAbsent(v.getKey(), k -> new ArrayList<>());
                for (int i = 0; i < v.getTsValueCount(); i++) {
                    @NotNull Object[] value = new Object[2];
                    TbSubscriptionUpdateTsValue tsValue = v.getTsValue(i);
                    value[0] = tsValue.getTs();
                    value[1] = tsValue.hasValue() ? tsValue.getValue() : null;
                    values.add(value);
                }
            });
            return new TelemetrySubscriptionUpdate(proto.getSubscriptionId(), data);
        }
    }

    @NotNull
    public static AlarmSubscriptionUpdate fromProto(@NotNull TransportProtos.TbAlarmSubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new AlarmSubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            Alarm alarm = JacksonUtil.fromString(proto.getAlarm(), Alarm.class);
            return new AlarmSubscriptionUpdate(proto.getSubscriptionId(), alarm);
        }
    }


    public static ToCoreMsg toTimeseriesUpdateProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, @NotNull List<TsKvEntry> ts) {
        TbTimeSeriesUpdateProto.Builder builder = TbTimeSeriesUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        ts.forEach(v -> builder.addData(toKeyValueProto(v.getTs(), v).build()));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setTsUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toTimeseriesDeleteProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, List<String> keys) {
        TbTimeSeriesDeleteProto.Builder builder = TbTimeSeriesDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.addAllKeys(keys);
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setTsDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAttributesUpdateProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, @NotNull List<AttributeKvEntry> attributes) {
        TbAttributeUpdateProto.Builder builder = TbAttributeUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        attributes.forEach(v -> builder.addData(toKeyValueProto(v.getLastUpdateTs(), v).build()));

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAttributesDeleteProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, String scope, List<String> keys, boolean notifyDevice) {
        TbAttributeDeleteProto.Builder builder = TbAttributeDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        builder.addAllKeys(keys);
        builder.setNotifyDevice(notifyDevice);

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }


    private static TsKvProto.Builder toKeyValueProto(long ts, @NotNull KvEntry attr) {
        KeyValueProto.Builder dataBuilder = KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setType(KeyValueType.forNumber(attr.getDataType().ordinal()));
        switch (attr.getDataType()) {
            case BOOLEAN:
                attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
                break;
            case LONG:
                attr.getLongValue().ifPresent(dataBuilder::setLongV);
                break;
            case DOUBLE:
                attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
                break;
            case JSON:
                attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
                break;
            case STRING:
                attr.getStrValue().ifPresent(dataBuilder::setStringV);
                break;
        }
        return TsKvProto.newBuilder().setTs(ts).setKv(dataBuilder);
    }

    public static EntityId toEntityId(String entityType, long entityIdMSB, long entityIdLSB) {
        return EntityIdFactory.getByTypeAndUuid(entityType, new UUID(entityIdMSB, entityIdLSB));
    }

    @NotNull
    public static List<TsKvEntry> toTsKvEntityList(@NotNull List<TsKvProto> dataList) {
        @NotNull List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), getKvEntry(proto.getKv()))));
        return result;
    }

    @NotNull
    public static List<AttributeKvEntry> toAttributeKvList(@NotNull List<TsKvProto> dataList) {
        @NotNull List<AttributeKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(getKvEntry(proto.getKv()), proto.getTs())));
        return result;
    }

    @Nullable
    private static KvEntry getKvEntry(@NotNull KeyValueProto proto) {
        @Nullable KvEntry entry = null;
        DataType type = DataType.values()[proto.getType().getNumber()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolV());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongV());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStringV());
                break;
            case JSON:
                entry = new JsonDataEntry(proto.getKey(), proto.getJsonV());
                break;
        }
        return entry;
    }

    public static ToCoreMsg toAlarmUpdateProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, Alarm alarm) {
        TbAlarmUpdateProto.Builder builder = TbAlarmUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAlarmDeletedProto(@NotNull TenantId tenantId, @NotNull EntityId entityId, Alarm alarm) {
        TbAlarmDeleteProto.Builder builder = TbAlarmDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }
}
