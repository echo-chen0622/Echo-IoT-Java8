package org.echoiot.server.service.apiusage;

import org.echoiot.rule.engine.api.MailService;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTbApiUsageStateServiceTest {

    @Mock
    TenantService tenantService;
    @Mock
    TimeseriesService tsService;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    TenantApiUsageState tenantUsageStateMock;
    @Mock
    ApiUsageStateService apiUsageStateService;
    @Mock
    TbTenantProfileCache tenantProfileCache;
    @Mock
    MailService mailService;
    @Mock
    DbCallbackExecutorService dbExecutor;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112"));

    DefaultTbApiUsageStateService service;

    @Before
    public void setUp() {
        service = Mockito.spy(new DefaultTbApiUsageStateService(clusterService, partitionService, tenantService, tsService, apiUsageStateService, tenantProfileCache, mailService, dbExecutor));
    }

    @Test
    public void givenTenantIdFromEntityStatesMap_whenGetApiUsageState() {
        service.myUsageStates.put(tenantId, tenantUsageStateMock);
        ApiUsageState tenantUsageState = service.getApiUsageState(tenantId);
        assertThat(tenantUsageState, is(tenantUsageStateMock.getApiUsageState()));
        Mockito.verify(service, never()).getOrFetchState(tenantId, tenantId);
    }

}
