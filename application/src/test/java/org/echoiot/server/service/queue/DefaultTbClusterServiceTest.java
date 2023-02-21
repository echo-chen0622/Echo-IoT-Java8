package org.echoiot.server.service.queue;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.service.gateway_device.GatewayNotificationsService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbClusterService.class)
public class DefaultTbClusterServiceTest {

    public static final String MONOLITH = "monolith";

    public static final String CORE = "core";

    public static final String RULE_ENGINE = "rule_engine";

    public static final String TRANSPORT = "transport";

    @MockBean
    protected DataDecodingEncodingService encodingService;
    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;
    @MockBean
    protected TbAssetProfileCache assetProfileCache;
    @MockBean
    protected GatewayNotificationsService gatewayNotificationsService;
    @MockBean
    protected PartitionService partitionService;
    @MockBean
    protected TbQueueProducerProvider producerProvider;

    @SpyBean
    protected NotificationsTopicService notificationsTopicService;
    @SpyBean
    protected TbClusterService clusterService;

    @Test
    public void testOnQueueChangeSingleMonolith() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueueChange(createTestQueue());

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(notificationsTopicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(notificationsTopicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeMultipleMonoliths() {
        @NotNull String monolith1 = MONOLITH + 1;
        @NotNull String monolith2 = MONOLITH + 2;
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueueChange(createTestQueue());

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(notificationsTopicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(notificationsTopicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeSingleMonolithAndSingleRemoteTransport() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH, TRANSPORT));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueueChange(createTestQueue());

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT);
        verify(notificationsTopicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());

        verify(tbREQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
    }

    @Test
    public void testOnQueueChangeMultipleMicroservices() {
        @NotNull String monolith1 = MONOLITH + 1;
        @NotNull String monolith2 = MONOLITH + 2;

        @NotNull String core1 = CORE + 1;
        @NotNull String core2 = CORE + 2;

        @NotNull String ruleEngine1 = RULE_ENGINE + 1;
        @NotNull String ruleEngine2 = RULE_ENGINE + 2;

        @NotNull String transport1 = TRANSPORT + 1;
        @NotNull String transport2 = TRANSPORT + 2;

        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2, ruleEngine1, ruleEngine2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2, core1, core2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2, transport1, transport2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> tbCoreQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTbCoreNotificationsMsgProducer()).thenReturn(tbCoreQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueueChange(createTestQueue());

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2);

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core1);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core2);
        verify(notificationsTopicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith1);
        verify(notificationsTopicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith2);

        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1);
        verify(notificationsTopicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2);
        verify(notificationsTopicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1);
        verify(notificationsTopicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2);

        verify(tbREQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbCoreQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, core1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, core2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, times(1))
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2)), any(TbProtoQueueMsg.class), isNull());
    }

    @NotNull
    protected Queue createTestQueue() {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        @NotNull Queue queue = new Queue(new QueueId(UUID.randomUUID()));
        queue.setTenantId(tenantId);
        queue.setName(DataConstants.MAIN_QUEUE_NAME);
        queue.setTopic("main");
        queue.setPartitions(10);
        return queue;
    }
}
