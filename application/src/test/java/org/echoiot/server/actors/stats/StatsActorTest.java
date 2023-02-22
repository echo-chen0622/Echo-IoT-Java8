package org.echoiot.server.actors.stats;

import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.event.EventService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

class StatsActorTest {

    StatsActor statsActor;
    ActorSystemContext actorSystemContext;
    EventService eventService;
    TbServiceInfoProvider serviceInfoProvider;

    @BeforeEach
    void setUp() {
        actorSystemContext = mock(ActorSystemContext.class);

        eventService = mock(EventService.class);
        willReturn(eventService).given(actorSystemContext).getEventService();
        serviceInfoProvider = mock(TbServiceInfoProvider.class);
        willReturn(serviceInfoProvider).given(actorSystemContext).getServiceInfoProvider();

        statsActor = new StatsActor(actorSystemContext);
    }

    @Test
    void givenEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        StatsPersistMsg emptyStats = new StatsPersistMsg(0, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
        statsActor.onStatsPersistMsg(emptyStats);
        verify(actorSystemContext, never()).getEventService();
    }

    @Test
    void givenNonEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        statsActor.onStatsPersistMsg(new StatsPersistMsg(0, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(1)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(2)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(3)).saveAsync(any(Event.class));
    }

}
