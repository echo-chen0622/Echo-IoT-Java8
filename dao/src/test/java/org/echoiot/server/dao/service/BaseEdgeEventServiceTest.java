package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.common.data.page.TimePageLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeEventId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

public abstract class BaseEdgeEventServiceTest extends AbstractServiceTest {

    long timeBeforeStartTime;
    long startTime;
    long eventTime;
    long endTime;
    long timeAfterEndTime;

    @Before
    public void before() throws ParseException {
        timeBeforeStartTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T11:30:00Z").getTime();
        startTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:00:00Z").getTime();
        eventTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:30:00Z").getTime();
        endTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:00:00Z").getTime();
        timeAfterEndTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:30:30Z").getTime();
    }

    @Test
    public void saveEdgeEvent() throws Exception {
        @NotNull EdgeId edgeId = new EdgeId(Uuids.timeBased());
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());
        @NotNull TenantId tenantId = new TenantId(Uuids.timeBased());
        @NotNull EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, deviceId, EdgeEventActionType.ADDED);
        edgeEventService.saveAsync(edgeEvent).get();

        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, new TimePageLink(1), false);
        Assert.assertFalse(edgeEvents.getData().isEmpty());

        EdgeEvent saved = edgeEvents.getData().get(0);
        Assert.assertEquals(saved.getTenantId(), edgeEvent.getTenantId());
        Assert.assertEquals(saved.getEdgeId(), edgeEvent.getEdgeId());
        Assert.assertEquals(saved.getEntityId(), edgeEvent.getEntityId());
        Assert.assertEquals(saved.getType(), edgeEvent.getType());
        Assert.assertEquals(saved.getAction(), edgeEvent.getAction());
        Assert.assertEquals(saved.getBody(), edgeEvent.getBody());
    }

    @NotNull
    protected EdgeEvent generateEdgeEvent(@Nullable TenantId tenantId, EdgeId edgeId, @NotNull EntityId entityId, EdgeEventActionType edgeEventAction) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        @NotNull EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEntityId(entityId.getId());
        edgeEvent.setType(EdgeEventType.DEVICE);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(readFromResource("TestJsonData.json"));
        return edgeEvent;
    }

    @Test
    public void findEdgeEventsByTimeDescOrder() throws Exception {
        @NotNull EdgeId edgeId = new EdgeId(Uuids.timeBased());
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

        @NotNull List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEventWithProvidedTime(timeBeforeStartTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 1, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 2, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(timeAfterEndTime, edgeId, deviceId, tenantId));

        Futures.allAsList(futures).get();

        @NotNull TimePageLink pageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime", SortOrder.Direction.DESC), startTime, endTime);
        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(2, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime + 2), edgeEvents.getData().get(0).getUuidId());
        Assert.assertEquals(Uuids.startOf(eventTime + 1), edgeEvents.getData().get(1).getUuidId());
        Assert.assertTrue(edgeEvents.hasNext());
        Assert.assertNotNull(pageLink.nextPageLink());

        edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink.nextPageLink(), true);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(1, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime), edgeEvents.getData().get(0).getUuidId());
        Assert.assertFalse(edgeEvents.hasNext());

        edgeEventService.cleanupEvents(1);
    }

    @Test
    public void findEdgeEventsWithTsUpdateAndWithout() throws Exception {
        @NotNull EdgeId edgeId = new EdgeId(Uuids.timeBased());
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        @NotNull TimePageLink pageLink = new TimePageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC));

        @NotNull EdgeEvent edgeEventWithTsUpdate = generateEdgeEvent(tenantId, edgeId, deviceId, EdgeEventActionType.TIMESERIES_UPDATED);
        edgeEventService.saveAsync(edgeEventWithTsUpdate).get();

        PageData<EdgeEvent> allEdgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);
        PageData<EdgeEvent> edgeEventsWithoutTsUpdate = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, false);

        Assert.assertNotNull(allEdgeEvents.getData());
        Assert.assertNotNull(edgeEventsWithoutTsUpdate.getData());
        Assert.assertEquals(1, allEdgeEvents.getData().size());
        Assert.assertEquals(allEdgeEvents.getData().get(0).getUuidId(), edgeEventWithTsUpdate.getUuidId());
        Assert.assertTrue(edgeEventsWithoutTsUpdate.getData().isEmpty());
    }

    private ListenableFuture<Void> saveEdgeEventWithProvidedTime(long time, EdgeId edgeId, @NotNull EntityId entityId, TenantId tenantId) throws Exception {
        @NotNull EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, entityId, EdgeEventActionType.ADDED);
        edgeEvent.setId(new EdgeEventId(Uuids.startOf(time)));
        return edgeEventService.saveAsync(edgeEvent);
    }
}
