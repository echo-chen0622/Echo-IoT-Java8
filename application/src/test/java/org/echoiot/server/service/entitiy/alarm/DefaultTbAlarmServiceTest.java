package org.echoiot.server.service.entitiy.alarm;

import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.service.entitiy.TbNotificationEntityService;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;
import org.echoiot.server.service.telemetry.AlarmSubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmServiceTest {

    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    @MockBean
    protected TbNotificationEntityService notificationEntityService;
    @MockBean
    protected EdgeService edgeService;
    @MockBean
    protected AlarmService alarmService;
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected TbClusterService tbClusterService;
    @MockBean
    private EntitiesVersionControlService vcService;

    @SpyBean
    DefaultTbAlarmService service;

    @Test
    public void testSave() throws EchoiotException {
        @NotNull var alarm = new Alarm();
        when(alarmSubscriptionService.createOrUpdateAlarm(alarm)).thenReturn(alarm);
        service.save(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).createOrUpdateAlarm(eq(alarm));
    }

    @Test
    public void testAck() {
        @NotNull var alarm = new Alarm();
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        when(alarmSubscriptionService.ackAlarm(any(), any(), anyLong())).thenReturn(Futures.immediateFuture(true));
        service.ack(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).ackAlarm(any(), any(), anyLong());
    }

    @Test
    public void testClear() {
        @NotNull var alarm = new Alarm();
        alarm.setStatus(AlarmStatus.ACTIVE_ACK);
        when(alarmSubscriptionService.clearAlarm(any(), any(), any(), anyLong())).thenReturn(Futures.immediateFuture(true));
        service.clear(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).clearAlarm(any(), any(), any(), anyLong());
    }

    @Test
    public void testDelete() {
        service.delete(new Alarm(), new User());

        verify(notificationEntityService, times(1)).notifyDeleteAlarm(any(), any(), any(), any(), any(), any(), anyString());
        verify(alarmSubscriptionService, times(1)).deleteAlarm(any(), any());
    }
}
