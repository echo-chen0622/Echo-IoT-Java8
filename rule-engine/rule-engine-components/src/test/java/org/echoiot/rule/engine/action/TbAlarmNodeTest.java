package org.echoiot.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.NotImplementedException;
import org.echoiot.common.util.ListeningExecutor;
import org.echoiot.rule.engine.api.*;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.echoiot.server.common.data.DataConstants.*;
import static org.echoiot.server.common.data.alarm.AlarmSeverity.CRITICAL;
import static org.echoiot.server.common.data.alarm.AlarmSeverity.WARNING;
import static org.echoiot.server.common.data.alarm.AlarmStatus.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TbAlarmNodeTest {

    private TbAbstractAlarmNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineAlarmService alarmService;

    @Mock
    private ScriptEngine detailsJs;

    @Captor
    private ArgumentCaptor<Runnable> successCaptor;
    @Captor
    private ArgumentCaptor<Consumer<Throwable>> failureCaptor;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final EntityId alarmOriginator = new AlarmId(Uuids.timeBased());
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    private final TbMsgMetaData metaData = new TbMsgMetaData();
    private final String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

    @Before
    public void before() {
        dbExecutor = new ListeningExecutor() {
            @NotNull
            @Override
            public <T> ListenableFuture<T> executeAsync(@NotNull Callable<T> task) {
                try {
                    return Futures.immediateFuture(task.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void execute(@NotNull Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void newAlarmCanBeCreated() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));
        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));
        long ts = msg.getTs();
        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void buildDetailsThrowsException() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFailedFuture(new NotImplementedException("message")));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createScriptEngine(ScriptLanguage.JS, "DETAILS");
        verify(ctx).getAlarmService();
        verify(ctx, times(3)).getDbCallbackExecutor();
        verify(ctx).logJsEvalRequest();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    @Test
    public void ifAlarmClearedCreateNew() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm clearedAlarm = Alarm.builder().status(CLEARED_ACK).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(clearedAlarm));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());


        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeUpdated() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(activeAlarm));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(activeAlarm);

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Updated"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_EXISTING_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        assertTrue(activeAlarm.getEndTs() > oldEndDate);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .endTs(activeAlarm.getEndTs())
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeCleared() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(activeAlarm));
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), nullable(JsonNode.class), anyLong()))
                .thenReturn(Futures.immediateFuture( false));
        when(alarmService.findAlarmByIdAsync(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()))).thenReturn(Futures.immediateFuture(activeAlarm));
//        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(activeAlarm);

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(CLEARED_UNACK)
                .severity(WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeClearedWithAlarmOriginator() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", alarmOriginator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        @NotNull AlarmId id = new AlarmId(alarmOriginator.getId());
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();
        activeAlarm.setId(id);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findAlarmByIdAsync(tenantId, id)).thenReturn(Futures.immediateFuture(activeAlarm));
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), nullable(JsonNode.class), anyLong())).thenReturn(Futures.immediateFuture(true));
//        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(activeAlarm);

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(alarmOriginator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(CLEARED_UNACK)
                .severity(WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();
        expectedAlarm.setId(id);

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmWithDynamicSeverityFromMessageBody() throws Exception {
        @NotNull TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setSeverity("$[alarmSeverity]");
        config.setAlarmType("SomeType");
        config.setScriptLang(ScriptLanguage.JS);
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        @NotNull String rawJson = "{\"alarmSeverity\": \"WARNING\", \"passed\": 5}";
        metaData.putValue("key", "value");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));
        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmWithDynamicSeverityFromMetadata() throws Exception {
        @NotNull TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setScriptLang(ScriptLanguage.JS);
        config.setSeverity("${alarmSeverity}");
        config.setAlarmType("SomeType");
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        metaData.putValue("alarmSeverity", "WARNING");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));
        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmsWithPropagationToTenantWithDynamicTypes() throws Exception{
        for (int i = 0; i < 10; i++) {
            @NotNull var config = new TbCreateAlarmNodeConfiguration();
            config.setPropagateToTenant(true);
            config.setSeverity(CRITICAL.name());
            config.setAlarmType("SomeType" + i);
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            config.setDynamicSeverity(true);
            @NotNull ObjectMapper mapper = new ObjectMapper();
            @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);

            metaData.putValue("key", "value");
            @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

            when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
            when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType" + i)).thenReturn(Futures.immediateFuture(null));
            doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));
            long ts = msg.getTs();
            node.onMsg(ctx, msg);

            verify(ctx, atMost(10)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, atMost(10)).tellNext(any(), eq("Created"));

            @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
            @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
            @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
            @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
            verify(ctx, atMost(10)).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

            assertEquals("ALARM", typeCaptor.getValue());
            assertEquals(originator, originatorCaptor.getValue());
            assertEquals("value", metadataCaptor.getValue().getValue("key"));
            assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
            assertNotSame(metaData, metadataCaptor.getValue());

            Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
            Alarm expectedAlarm = Alarm.builder()
                    .startTs(ts)
                    .endTs(ts)
                    .tenantId(tenantId)
                    .originator(originator)
                    .status(ACTIVE_UNACK)
                    .severity(CRITICAL)
                    .propagateToTenant(true)
                    .type("SomeType" + i)
                    .details(null)
                    .build();

            assertEquals(expectedAlarm, actualAlarm);
        }
    }

    private void initWithCreateAlarmScript() {
        try {
            @NotNull TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
            config.setPropagate(true);
            config.setSeverity(CRITICAL.name());
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            @NotNull ObjectMapper mapper = new ObjectMapper();
            @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void initWithClearAlarmScript() {
        try {
            @NotNull TbClearAlarmNodeConfiguration config = new TbClearAlarmNodeConfiguration();
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            @NotNull ObjectMapper mapper = new ObjectMapper();
            @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbClearAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        @NotNull ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }

}
