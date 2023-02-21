package org.echoiot.rule.engine.math;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.echoiot.common.util.AbstractListeningExecutor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.RuleEngineTelemetryService;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TbMathNodeTest {

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

    @Mock
    private TbContext ctx;
    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService tsService;
    @Mock
    private RuleEngineTelemetryService telemetryService;
    private AbstractListeningExecutor dbExecutor;

    @Before
    public void before() {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();
        initMocks();
    }

    @After
    public void after() {
        dbExecutor.destroy();
    }

    private void initMocks() {
        Mockito.reset(ctx);
        Mockito.reset(attributesService);
        Mockito.reset(tsService);
        Mockito.reset(telemetryService);
        lenient().when(ctx.getAttributesService()).thenReturn(attributesService);
        lenient().when(ctx.getTelemetryService()).thenReturn(telemetryService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(tsService);
        lenient().when(ctx.getTenantId()).thenReturn(tenantId);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);
    }

    @NotNull
    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, TbMathResult result, TbMathArgument... arguments) {
        return initNode(operation, null, result, arguments);
    }

    @NotNull
    private TbMathNode initNodeWithCustomFunction(String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(TbRuleNodeMathFunctionType.CUSTOM, expression, result, arguments);
    }

    @NotNull
    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, String expression, TbMathResult result, TbMathArgument... arguments) {
        try {
            @NotNull TbMathNodeConfiguration configuration = new TbMathNodeConfiguration();
            configuration.setOperation(operation);
            if (TbRuleNodeMathFunctionType.CUSTOM.equals(operation)) {
                configuration.setCustomFunction(expression);
            }
            configuration.setResult(result);
            configuration.setArguments(Arrays.asList(arguments));
            @NotNull TbMathNode node = new TbMathNode();
            node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));
            return node;
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    public void testExp4j() {
        @NotNull var node = initNodeWithCustomFunction("2a+3b",
                                                       new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                                                       new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                                                       new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
                                                      );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(10, resultJson.get("result").asInt());
    }

    @Test
    public void testSimpleFunctions() {
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.ADD, 2.1, 2.2, 4.3);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.SUB, 2.1, 2.2, -0.1);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MULT, 2.1, 2.0, 4.2);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.DIV, 4.2, 2.0, 2.1);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIN, Math.toRadians(30), 0.5);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIN, Math.toRadians(90), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SINH, Math.toRadians(0), 0.0);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COSH, Math.toRadians(0), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COS, Math.toRadians(60), 0.5);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COS, Math.toRadians(0), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TAN, Math.toRadians(45), 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TAN, Math.toRadians(0), 0);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TANH, 90, 1);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ACOS, 0.5, 1.05);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ASIN, 0.5, 0.52);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ATAN, 0.5, 0.46);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.ATAN2, 0.5, 0.3, 1.03);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.EXP, 1, 2.72);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.EXPM1, 1, 1.72);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ABS, -1, 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SQRT, 4, 2);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.CBRT, 8, 2);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.GET_EXP, 4, 2);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.HYPOT, 4, 5, 6.4);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG, 4, 1.39);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG10, 4, 0.6);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG1P, 4, 1.61);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.CEIL, 1.55, 2);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.FLOOR, 23.97, 23);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.FLOOR_DIV, 5, 3, 1);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.FLOOR_MOD, 6, 3, 0);

        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MIN, 5, 3, 3);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MAX, 5, 3, 5);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.POW, 5, 3, 125);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIGNUM, 0.55, 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.RAD, 5, 0.09);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.DEG, 5, 286.48);
    }

    private void testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double arg2, double result) {
        initMocks();

        @NotNull var node = initNode(function,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", arg1).put("b", arg2).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000).times(1)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    private void testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double result) {
        initMocks();

        @NotNull var node = initNode(function,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", arg1).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    @Test
    public void test_2_plus_2_body() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.ADD,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(4, resultJson.get("result").asInt());
    }

    @Test
    public void test_2_plus_2_meta() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.ADD,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 0, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        Assert.assertNotNull(resultMsg.getMetaData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("4", result);
    }

    @Test
    public void test_2_plus_2_attr_and_ts() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.ADD,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.ATTRIBUTE, "a"),
                                     new TbMathArgument(TbMathArgumentType.TIME_SERIES, "b")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().toString());

        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SERVER_SCOPE, "a"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("a", 2.0)))));

        Mockito.when(tsService.findLatest(tenantId, originator, "b"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("b", 2L)))));

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(4, resultJson.get("result").asInt());
    }

    @Test
    public void test_sqrt_5_body() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    @Test
    public void test_sqrt_5_meta() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_to_attribute_and_metadata() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.ATTRIBUTE, "result", 3, false, true, DataConstants.SERVER_SCOPE),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        Mockito.when(telemetryService.saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble()))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_to_timeseries_and_data() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());
        Mockito.when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    @Test
    public void test_sqrt_5_to_timeseries_and_metadata_and_data() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, true, DataConstants.SERVER_SCOPE),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());
        Mockito.when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultMetadata = resultMsg.getMetaData().getValue("result");
        var resultData = JacksonUtil.toJsonNode(resultMsg.getData());

        Assert.assertTrue(resultData.has("result"));
        Assert.assertEquals(2.236, resultData.get("result").asDouble(), 0.0);

        Assert.assertNotNull(resultMetadata);
        Assert.assertEquals("2.236", resultMetadata);
    }

    @Test
    public void test_sqrt_5_default_value() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        tbMathArgument.setDefaultValue(5.0);
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                                     tbMathArgument
                                    );
        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 10).toString());

        node.onMsg(ctx, msg);
        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_default_value_failure() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey")
                                    );
        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 10).toString());
        Throwable thrown = assertThrows(RuntimeException.class, () -> {
            node.onMsg(ctx, msg);
        });
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void testConvertMsgBodyIfRequiredFailure() {
        @NotNull var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                                     new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                                     new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
                                    );

        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), "[]");
        Throwable thrown = assertThrows(RuntimeException.class, () -> {
            node.onMsg(ctx, msg);
        });
        Assert.assertNotNull(thrown.getMessage());
    }
}
