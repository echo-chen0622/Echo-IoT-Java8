package org.echoiot.rule.engine.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.rule.engine.api.*;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.model.sql.AttributeKvCompositeKey;
import org.echoiot.server.dao.model.sql.AttributeKvEntity;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbDeviceProfileNodeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbDeviceProfileNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineDeviceProfileCache cache;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private RuleEngineAlarmService alarmService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private AttributesService attributesService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Test
    public void testRandomMessageType() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        @NotNull TbMsg msg = TbMsg.newMsg("123456789", deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAlarmCreate() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        @NotNull AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        @NotNull AlarmRule clearRule = new AlarmRule();
        @NotNull AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
        clearRule.setCondition(clearCondition);
        dpa.setClearRule(clearRule);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        @NotNull TbMsg theMsg2 = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "2");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg2);


        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg2, "Alarm Updated");

    }

    @Test
    public void testConstantKeyFilterSimple() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarmEnabled"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setBooleanValue(Boolean.TRUE);
        attributeKvEntity.setLastUpdateTs(System.currentTimeMillis());

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> attrListListenableFuture = Futures.immediateFuture(Collections.singletonList(entry));

        @NotNull AlarmConditionFilter alarmEnabledFilter = new AlarmConditionFilter();
        alarmEnabledFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"));
        alarmEnabledFilter.setValue(Boolean.TRUE);
        alarmEnabledFilter.setValueType(EntityKeyValueType.BOOLEAN);
        @NotNull BooleanFilterPredicate alarmEnabledPredicate = new BooleanFilterPredicate();
        alarmEnabledPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        alarmEnabledPredicate.setValue(new FilterPredicateValue<>(
                Boolean.FALSE,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarmEnabled")
        ));
        alarmEnabledFilter.setPredicate(alarmEnabledPredicate);

        @NotNull AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperaturePredicate.setValue(new FilterPredicateValue<>(20.0, null, null));
        temperatureFilter.setPredicate(temperaturePredicate);

        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Arrays.asList(alarmEnabledFilter, temperatureFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("alarmEnabledAlarmID");
        dpa.setAlarmType("alarmEnabledAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(attrListListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testConstantKeyFilterInherited() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, tenantId.getId(), "SERVER_SCOPE", "alarmEnabled"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setBooleanValue(Boolean.TRUE);
        attributeKvEntity.setLastUpdateTs(System.currentTimeMillis());

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> attrListListenableFuture = Futures.immediateFuture(Optional.of(entry));

        @NotNull AlarmConditionFilter alarmEnabledFilter = new AlarmConditionFilter();
        alarmEnabledFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"));
        alarmEnabledFilter.setValue(Boolean.TRUE);
        alarmEnabledFilter.setValueType(EntityKeyValueType.BOOLEAN);
        @NotNull BooleanFilterPredicate alarmEnabledPredicate = new BooleanFilterPredicate();
        alarmEnabledPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        alarmEnabledPredicate.setValue(new FilterPredicateValue<>(
                Boolean.FALSE,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarmEnabled", true)
        ));
        alarmEnabledFilter.setPredicate(alarmEnabledPredicate);

        @NotNull AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperaturePredicate.setValue(new FilterPredicateValue<>(20.0, null, null));
        temperatureFilter.setPredicate(temperaturePredicate);

        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Arrays.asList(alarmEnabledFilter, temperatureFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("alarmEnabledAlarmID");
        dpa.setAlarmType("alarmEnabledAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(attrListListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicValue() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicDurationValue() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarm_delay"
        );

        @NotNull AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        long alarmDelayInSeconds = 5L;
        alarmDelayAttributeKvEntity.setLongValue(alarmDelayInSeconds);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Arrays.asList(entry, alarmDelayAttributeKvEntry));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                10L,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", false)
        );

        @NotNull DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay);

        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInheritTenantAttributeForDuration() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);


        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarm_delay"
        );

        @NotNull AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        long alarmDelayInSeconds = 5L;
        alarmDelayAttributeKvEntity.setLongValue(alarmDelayInSeconds);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalDurationAttribute =
                Futures.immediateFuture(Optional.of(alarmDelayAttributeKvEntry));
        @NotNull ListenableFuture<List<AttributeKvEntry>> listNoDurationAttribute =
                Futures.immediateFuture(Collections.singletonList(entry));
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> emptyOptional =
                Futures.immediateFuture(Optional.empty());

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                10L,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", true)
        );

        @NotNull DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(optionalDurationAttribute);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(emptyOptional);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listNoDurationAttribute);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay);

        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicRepeatingValue() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarm_delay"
        );

        @NotNull AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        long alarmRepeating = 2;
        alarmDelayAttributeKvEntity.setLongValue(alarmRepeating);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Arrays.asList(entry, alarmDelayAttributeKvEntry));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", false)
        );


        @NotNull RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        data.put("temperature", 151);
        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInheritTenantAttributeForRepeating() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarm_delay"
        );

        @NotNull AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        long repeatingCondition = 2;
        alarmDelayAttributeKvEntity.setLongValue(repeatingCondition);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalDurationAttribute =
                Futures.immediateFuture(Optional.of(alarmDelayAttributeKvEntry));
        @NotNull ListenableFuture<List<AttributeKvEntry>> listNoDurationAttribute =
                Futures.immediateFuture(Collections.singletonList(entry));
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> emptyOptional =
                Futures.immediateFuture(Optional.empty());

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", true)
        );

        @NotNull RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(optionalDurationAttribute);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(emptyOptional);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listNoDurationAttribute);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        data.put("temperature", 151);
        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForUseDefaultDurationWhenDynamicDurationValueIsNull() throws Exception {
        init();

        long alarmDelayInSeconds = 5;
        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Collections.singletonList(entry));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                alarmDelayInSeconds,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, null, false)
        );

        @NotNull DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay);

        @NotNull TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                           TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForUseDefaultRepeatingWhenDynamicDurationValueIsNull() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Collections.singletonList(entry));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull RepeatingAlarmConditionSpec repeating = new RepeatingAlarmConditionSpec();
        repeating.setPredicate(new FilterPredicateValue<>(
                0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_rule", false)
        ));
        alarmCondition.setSpec(repeating);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testActiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsInactive() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKeyActiveSchedule = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "dynamicValueActiveSchedule"
        );

        @NotNull AttributeKvEntity attributeKvEntityActiveSchedule = new AttributeKvEntity();
        attributeKvEntityActiveSchedule.setId(compositeKeyActiveSchedule);
        attributeKvEntityActiveSchedule.setJsonValue(
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":true,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":8.64e+7}],\"dynamicValue\":null}"
        );
        attributeKvEntityActiveSchedule.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entryActiveSchedule = attributeKvEntityActiveSchedule.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureActiveSchedule =
                Futures.immediateFuture(Collections.singletonList(entryActiveSchedule));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                null
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull CustomTimeSchedule schedule = new CustomTimeSchedule();
        schedule.setItems(Collections.emptyList());
        schedule.setDynamicValue(new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "dynamicValueActiveSchedule", false));

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        alarmRule.setSchedule(schedule);
        @NotNull DeviceProfileAlarm deviceProfileAlarmActiveSchedule = new DeviceProfileAlarm();
        deviceProfileAlarmActiveSchedule.setId("highTemperatureAlarmID");
        deviceProfileAlarmActiveSchedule.setAlarmType("highTemperatureAlarm");
        deviceProfileAlarmActiveSchedule.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(deviceProfileAlarmActiveSchedule));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureActiveSchedule);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

//        Mockito.reset(ctx);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInactiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsActive() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKeyInactiveSchedule = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "dynamicValueInactiveSchedule"
        );

        @NotNull AttributeKvEntity attributeKvEntityInactiveSchedule = new AttributeKvEntity();
        attributeKvEntityInactiveSchedule.setId(compositeKeyInactiveSchedule);
        attributeKvEntityInactiveSchedule.setJsonValue(
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":false,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":0}],\"dynamicValue\":null}"
        );

        attributeKvEntityInactiveSchedule.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entryInactiveSchedule = attributeKvEntityInactiveSchedule.toData();

        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureInactiveSchedule =
                Futures.immediateFuture(Collections.singletonList(entryInactiveSchedule));

        @NotNull AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                null
        ));

        highTempFilter.setPredicate(highTemperaturePredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        @NotNull CustomTimeSchedule schedule = new CustomTimeSchedule();

        @NotNull List<CustomTimeScheduleItem> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            @NotNull CustomTimeScheduleItem item = new CustomTimeScheduleItem();
            item.setEnabled(true);
            item.setDayOfWeek(i + 1);
            item.setEndsOn(0);
            item.setStartsOn(0);
            items.add(item);
        }

        schedule.setItems(items);
        schedule.setDynamicValue(new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "dynamicValueInactiveSchedule", false));

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        alarmRule.setSchedule(schedule);
        @NotNull DeviceProfileAlarm deviceProfileAlarmNonactiveSchedule = new DeviceProfileAlarm();
        deviceProfileAlarmNonactiveSchedule.setId("highTemperatureAlarmID");
        deviceProfileAlarmNonactiveSchedule.setAlarmType("highTemperatureAlarm");
        deviceProfileAlarmNonactiveSchedule.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(deviceProfileAlarmNonactiveSchedule));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureInactiveSchedule);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentCustomersAttributeForDynamicValue() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.CUSTOMER, deviceId.getId(), "SERVER_SCOPE", "lessAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        @NotNull AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        20.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "lessAttribute"))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 25);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentTenantAttributeForDynamicValue() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "lessAttribute"
        );

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(50L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        @NotNull AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        32.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_TENANT, "lessAttribute"))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 40);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testTenantInheritModeForDynamicValues() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "tenantAttribute"
        );

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> emptyOptionalFuture =
                Futures.immediateFuture(Optional.empty());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        @NotNull AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "tenantAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(emptyOptionalFuture);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(DataConstants.SERVER_SCOPE),  Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

    }


    @Test
    public void testCustomerInheritModeForDynamicValues() throws Exception {
        init();

        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();

        @NotNull AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.DEVICE, deviceId.getId(), EntityKeyType.SERVER_ATTRIBUTE.name(), "tenantAttribute"
        );

        @NotNull Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        @NotNull AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        @NotNull AttributeKvEntry entry = attributeKvEntity.toData();
        @NotNull ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> emptyOptionalFuture =
                Futures.immediateFuture(Optional.empty());
        @NotNull ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        @NotNull AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "tenantAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        @NotNull DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("greaterTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "greaterTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(emptyOptionalFuture);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(DataConstants.SERVER_SCOPE),  Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        @NotNull TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    private void init() throws TbNodeException {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getDeviceProfileCache()).thenReturn(cache);
        Mockito.when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        Mockito.when(ctx.getAlarmService()).thenReturn(alarmService);
        Mockito.when(ctx.getDeviceService()).thenReturn(deviceService);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.createObjectNode());
        node = new TbDeviceProfileNode();
        node.init(ctx, nodeConfiguration);
    }

}
