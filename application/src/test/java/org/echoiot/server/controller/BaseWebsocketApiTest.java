package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.service.subscription.TbAttributeSubscriptionScope;
import org.echoiot.server.service.telemetry.TelemetrySubscriptionService;
import org.echoiot.server.service.telemetry.cmd.v2.EntityCountCmd;
import org.echoiot.server.service.telemetry.cmd.v2.EntityCountUpdate;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.echoiot.server.service.telemetry.sub.SubscriptionErrorCode;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class BaseWebsocketApiTest extends AbstractControllerTest {
    @Resource
    private TelemetrySubscriptionService tsService;

    Device device;
    DeviceTypeFilter dtf;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        device = new Device();
        device.setName("Device");
        device.setType("default");
        device.setLabel("testLabel" + (int) (Math.random() * 1000));
        device = doPost("/api/device", device, Device.class);
        dtf = new DeviceTypeFilter(device.getType(), device.getName());
    }

    @After
    public void tearDown() throws Exception {
        loginTenantAdmin();
        doDelete("/api/device/" + device.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testEntityDataHistoryWsCmd() throws Exception {
        @NotNull List<String> keys = List.of("temperature");
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendHistoryCmd(keys, now, TimeUnit.HOURS.toMillis(1), dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertEquals(0, pageData.getData().get(0).getTimeseries().get("temperature").length);

        @NotNull TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        @NotNull TsKvEntry dataPoint2 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(2), new LongDataEntry("temperature", 42L));
        @NotNull TsKvEntry dataPoint3 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(3), new LongDataEntry("temperature", 42L));
        @NotNull List<TsKvEntry> tsData = Arrays.asList(dataPoint1, dataPoint2, dataPoint3);

        sendTelemetry(device, tsData);

        update = getWsClient().sendHistoryCmd(keys, now, TimeUnit.HOURS.toMillis(1), dtf);

        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> dataList = update.getUpdate();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(1, dataList.size());
        Assert.assertEquals(device.getId(), dataList.get(0).getEntityId());
        TsValue[] tsArray = dataList.get(0).getTimeseries().get("temperature");
        Assert.assertEquals(3, tsArray.length);
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsArray[0]);
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsArray[1]);
        Assert.assertEquals(new TsValue(dataPoint3.getTs(), dataPoint3.getValueAsString()), tsArray[2]);
    }

    @Test
    public void testEntityDataTimeSeriesWsCmd() throws Exception {
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());

        @NotNull TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        @NotNull TsKvEntry dataPoint2 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(2), new LongDataEntry("temperature", 43L));
        @NotNull TsKvEntry dataPoint3 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(3), new LongDataEntry("temperature", 44L));
        @NotNull List<TsKvEntry> tsData = Arrays.asList(dataPoint1, dataPoint2, dataPoint3);

        sendTelemetry(device, tsData);

        update = getWsClient().subscribeTsUpdate(List.of("temperature"), now, TimeUnit.HOURS.toMillis(1));
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        TsValue[] tsArray = listData.get(0).getTimeseries().get("temperature");
        Assert.assertEquals(3, tsArray.length);
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsArray[0]);
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsArray[1]);
        Assert.assertEquals(new TsValue(dataPoint3.getTs(), dataPoint3.getValueAsString()), tsArray[2]);

        now = System.currentTimeMillis();
        @NotNull TsKvEntry dataPoint4 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 45L));
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint4));
        String msg = getWsClient().waitForUpdate();

        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getTimeseries());
        TsValue[] tsValues = eData.get(0).getTimeseries().get("temperature");
        Assert.assertNotNull(tsValues);
        Assert.assertEquals(new TsValue(dataPoint4.getTs(), dataPoint4.getValueAsString()), tsValues[0]);
    }

    @Test
    public void testEntityCountWsCmd() throws Exception {
        @NotNull AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Collections.singletonList(dataPoint1));

        @NotNull EntityCountQuery edq1 = new EntityCountQuery(dtf, Collections.emptyList());
        @NotNull EntityCountCmd cmd1 = new EntityCountCmd(1, edq1);

        getWsClient().send(cmd1);

        EntityCountUpdate update1 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update1.getCmdId());
        Assert.assertEquals(1, update1.getCount());

        @NotNull DeviceTypeFilter dtf2 = new DeviceTypeFilter("non-existing-device-type", "D");
        @NotNull EntityCountQuery edq2 = new EntityCountQuery(dtf2, Collections.emptyList());
        @NotNull EntityCountCmd cmd2 = new EntityCountCmd(2, edq2);

        getWsClient().send(cmd2);

        String msg2 = getWsClient().waitForReply();
        EntityCountUpdate update2 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(2, update2.getCmdId());
        Assert.assertEquals(0, update2.getCount());

        @NotNull KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(40));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        highTemperatureFilter.setValueType(EntityKeyValueType.NUMERIC);

        @NotNull DeviceTypeFilter dtf3 = new DeviceTypeFilter("default", "D");
        @NotNull EntityCountQuery edq3 = new EntityCountQuery(dtf3, Collections.singletonList(highTemperatureFilter));
        @NotNull EntityCountCmd cmd3 = new EntityCountCmd(3, edq3);
        getWsClient().send(cmd3);

        EntityCountUpdate update3 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(3, update3.getCmdId());
        Assert.assertEquals(1, update3.getCount());

        @NotNull KeyFilter highTemperatureFilter2 = new KeyFilter();
        highTemperatureFilter2.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        @NotNull NumericFilterPredicate predicate2 = new NumericFilterPredicate();
        predicate2.setValue(FilterPredicateValue.fromDouble(50));
        predicate2.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter2.setPredicate(predicate2);
        highTemperatureFilter2.setValueType(EntityKeyValueType.NUMERIC);

        @NotNull DeviceTypeFilter dtf4 = new DeviceTypeFilter("default", "D");
        @NotNull EntityCountQuery edq4 = new EntityCountQuery(dtf4, Collections.singletonList(highTemperatureFilter2));
        @NotNull EntityCountCmd cmd4 = new EntityCountCmd(4, edq4);

        getWsClient().send(cmd4);

        EntityCountUpdate update4 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(4, update4.getCmdId());
        Assert.assertEquals(0, update4.getCount());
    }

    @Test
    public void testEntityDataLatestWidgetFlow() throws Exception {
        @NotNull List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        long now = System.currentTimeMillis() - 100;

        @NotNull EntityDataQuery edq = new EntityDataQuery(dtf, new EntityDataPageLink(1, 0, null, null),
                                                           Collections.emptyList(), keys, Collections.emptyList());

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(edq);
        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());

        @NotNull TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        @NotNull List<TsKvEntry> tsData = List.of(dataPoint1);
        sendTelemetry(device, tsData);

        update = getWsClient().subscribeLatestUpdate(keys);

        Assert.assertEquals(1, update.getCmdId());

        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        @NotNull TsKvEntry dataPoint2 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 52L));

        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint2));

        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint1));
        String msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestTsWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        @NotNull List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());

        getWsClient().registerWaitForUpdate();
        @NotNull TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        @NotNull List<TsKvEntry> tsData = List.of(dataPoint1);
        sendTelemetry(device, tsData);

        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());

        Assert.assertEquals(1, update.getCmdId());

        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        @NotNull TsKvEntry dataPoint2 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 52L));
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint2));
        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint1));
        String msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestAttrWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        @NotNull List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "serverAttributeKey"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);
        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getValue());

        getWsClient().registerWaitForUpdate();

        @NotNull AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("serverAttributeKey", 42L));
        @NotNull List<AttributeKvEntry> tsData = List.of(dataPoint1);
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, tsData);

        String msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);
        update = mapper.readValue(msg, EntityDataUpdate.class);

        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint1.getLastUpdateTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        @NotNull AttributeKvEntry dataPoint2 = new BaseAttributeKvEntry(now, new LongDataEntry("serverAttributeKey", 52L));

        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(dataPoint2));
        msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);

        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint2.getLastUpdateTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestAttrTypesWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        @NotNull List<EntityKey> keys = List.of(
                new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "serverAttributeKey"),
                new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "clientAttributeKey"),
                new EntityKey(EntityKeyType.SHARED_ATTRIBUTE, "sharedAttributeKey"),
                new EntityKey(EntityKeyType.ATTRIBUTE, "anyAttributeKey"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey").getValue());

        getWsClient().registerWaitForUpdate();
        @NotNull AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("serverAttributeKey", 42L));
        @NotNull List<AttributeKvEntry> tsData = List.of(dataPoint1);

        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, tsData);

        String msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);
        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        TsValue attrValue = eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint1.getLastUpdateTs(), dataPoint1.getValueAsString()), attrValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SHARED_SCOPE, List.of(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, List.of(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        @NotNull AttributeKvEntry dataPoint2 = new BaseAttributeKvEntry(now, new LongDataEntry("sharedAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.SHARED_SCOPE, List.of(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint2.getLastUpdateTs(), dataPoint2.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        @NotNull AttributeKvEntry dataPoint3 = new BaseAttributeKvEntry(now, new LongDataEntry("clientAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, List.of(dataPoint3));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint3.getLastUpdateTs(), dataPoint3.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        @NotNull AttributeKvEntry dataPoint4 = new BaseAttributeKvEntry(now, new LongDataEntry("anyAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, List.of(dataPoint4));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint4.getLastUpdateTs(), dataPoint4.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        @NotNull AttributeKvEntry dataPoint5 = new BaseAttributeKvEntry(now, new LongDataEntry("anyAttributeKey", 43L));
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(dataPoint5));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint5.getLastUpdateTs(), dataPoint5.getValueAsString()), attrValue);
    }

    @Test
    public void testAttributesSubscription_sysAdmin() throws Exception {
        loginSysAdmin();
        @NotNull SingleEntityFilter entityFilter = new SingleEntityFilter();
        entityFilter.setSingleEntity(tenantId);

        assertThatNoException().isThrownBy(() -> {
            JsonNode update = getWsClient().subscribeForAttributes(tenantId, TbAttributeSubscriptionScope.SERVER_SCOPE.name(), List.of("attr"));
            assertThat(update.get("errorMsg").isNull()).isTrue();
            assertThat(update.get("errorCode").asInt()).isEqualTo(SubscriptionErrorCode.NO_ERROR.getCode());
        });

        getWsClient().registerWaitForUpdate();
        @NotNull String expectedAttrValue = "42";
        sendAttributes(TenantId.SYS_TENANT_ID, tenantId, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry("attr", expectedAttrValue))
        ));
        JsonNode update = JacksonUtil.toJsonNode(getWsClient().waitForUpdate());
        assertThat(update).isNotNull();
        assertThat(update.get("data").get("attr").get(0).get(1).asText()).isEqualTo(expectedAttrValue);
    }

    private void sendTelemetry(@NotNull Device device, List<TsKvEntry> tsData) throws InterruptedException {
        @NotNull CountDownLatch latch = new CountDownLatch(1);
        tsService.saveAndNotify(device.getTenantId(), null, device.getId(), tsData, 0, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                latch.countDown();
            }
        });
        latch.await(3, TimeUnit.SECONDS);
    }

    private void sendAttributes(@NotNull Device device, @NotNull TbAttributeSubscriptionScope scope, List<AttributeKvEntry> attrData) throws InterruptedException {
        sendAttributes(device.getTenantId(), device.getId(), scope, attrData);
    }

    private void sendAttributes(TenantId tenantId, EntityId entityId, @NotNull TbAttributeSubscriptionScope scope, List<AttributeKvEntry> attrData) throws InterruptedException {
        @NotNull CountDownLatch latch = new CountDownLatch(1);
        tsService.saveAndNotify(tenantId, entityId, scope.name(), attrData, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                latch.countDown();
            }
        });
        latch.await(3, TimeUnit.SECONDS);
    }
}
