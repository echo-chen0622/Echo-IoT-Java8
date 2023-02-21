package org.echoiot.server.service.state;

import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.DeviceIdInfo;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.query.EntityData;
import org.echoiot.server.common.data.query.EntityKeyType;
import org.echoiot.server.common.data.query.TsValue;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.UUID;

import static org.echoiot.server.service.state.DefaultDeviceStateService.INACTIVITY_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDeviceStateServiceTest {

    @Mock
    TenantService tenantService;
    @Mock
    DeviceService deviceService;
    @Mock
    AttributesService attributesService;
    @Mock
    TimeseriesService tsService;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    DeviceStateData deviceStateDataMock;
    @Mock
    TbServiceInfoProvider serviceInfoProvider;

    @NotNull
    DeviceId deviceId = DeviceId.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112");

    DefaultDeviceStateService service;

    @Before
    public void setUp() {
        service = Mockito.spy(new DefaultDeviceStateService(tenantService, deviceService, attributesService, tsService, clusterService, partitionService, serviceInfoProvider, null, null));
    }

    @Test
    public void givenDeviceIdFromDeviceStatesMap_whenGetOrFetchDeviceStateData_thenNoStackOverflow() {
        service.deviceStates.put(deviceId, deviceStateDataMock);
        @NotNull DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData, CoreMatchers.is(deviceStateDataMock));
        Mockito.verify(service, never()).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
    }

    @Test
    public void givenDeviceIdWithoutDeviceStateInMap_whenGetOrFetchDeviceStateData_thenFetchDeviceStateData() {
        service.deviceStates.clear();
        willReturn(deviceStateDataMock).given(service).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
        @NotNull DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData, CoreMatchers.is(deviceStateDataMock));
        Mockito.verify(service, times(1)).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
    }

    @Test
    public void givenPersistToTelemetryAndDefaultInactivityTimeoutFetched_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        @NotNull var latest =
                Map.of(
                        EntityKeyType.TIME_SERIES, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(defaultInactivityTimeoutInSec * 1000))),
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    @Test
    public void givenPersistToTelemetryAndNoInactivityTimeoutFetchedFromTimeSeries_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        @NotNull var latest =
                Map.of(
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    private void process(Map<EntityKeyType, Map<String, TsValue>> latest, long defaultInactivityTimeoutInSec) {
        service.setDefaultInactivityTimeoutInSec(defaultInactivityTimeoutInSec);
        service.setDefaultInactivityTimeoutMs(defaultInactivityTimeoutInSec * 1000);
        service.setPersistToTelemetry(true);

        @NotNull var deviceUuid = UUID.randomUUID();
        @NotNull var deviceId = new DeviceId(deviceUuid);

        DeviceStateData deviceStateData = service.toDeviceStateData(new EntityData(deviceId, latest, Map.of()), new DeviceIdInfo(TenantId.SYS_TENANT_ID.getId(), UUID.randomUUID(), deviceUuid));

        Assert.assertEquals(5000L, deviceStateData.getState().getInactivityTimeout());
    }

}
