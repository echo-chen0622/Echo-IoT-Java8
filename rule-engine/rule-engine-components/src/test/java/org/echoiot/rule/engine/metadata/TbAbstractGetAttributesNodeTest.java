package org.echoiot.rule.engine.metadata;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.echoiot.common.util.AbstractListeningExecutor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.BaseAttributeKvEntry;
import org.echoiot.server.common.data.kv.BasicTsKvEntry;
import org.echoiot.server.common.data.kv.JsonDataEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class TbAbstractGetAttributesNodeTest {

    final ObjectMapper mapper = new ObjectMapper();

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

    @Mock
    private TbContext ctx;
    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService tsService;
    private AbstractListeningExecutor dbExecutor;

    private List<String> clientAttributes;
    private List<String> serverAttributes;
    private List<String> sharedAttributes;
    private List<String> tsKeys;
    private long ts;

    @Before
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        Mockito.reset(ctx);
        Mockito.reset(attributesService);
        Mockito.reset(tsService);

        Mockito.reset(ctx);
        Mockito.reset(attributesService);
        Mockito.reset(tsService);

        lenient().when(ctx.getAttributesService()).thenReturn(attributesService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(tsService);
        lenient().when(ctx.getTenantId()).thenReturn(tenantId);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");
        ts = System.currentTimeMillis();

        Mockito.when(attributesService.find(tenantId, originator, DataConstants.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes, ts)));


        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes, ts)));


        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes, ts)));

        Mockito.when(tsService.findLatest(tenantId, originator, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTsKvEntry(tsKeys, ts)));
    }

    @After
    public void after() {
        dbExecutor.destroy();
    }

    @Test
    public void fetchToMetadata_whenOnMsg_then_success() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(false, false, false);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg resultMsg = checkMsg(true);

        //check attributes
        checkAttributes(resultMsg, false, "cs_", clientAttributes);
        checkAttributes(resultMsg, false, "ss_", serverAttributes);
        checkAttributes(resultMsg, false, "shared_", sharedAttributes);

        //check timeseries
        checkTs(resultMsg, false, false, tsKeys);
    }

    @Test
    public void fetchToMetadata_latestWithTs_whenOnMsg_then_success() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(false, true, false);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg resultMsg = checkMsg(true);

        //check attributes
        checkAttributes(resultMsg, false, "cs_", clientAttributes);
        checkAttributes(resultMsg, false, "ss_", serverAttributes);
        checkAttributes(resultMsg, false, "shared_", sharedAttributes);

        //check timeseries with ts
        checkTs(resultMsg, false, true, tsKeys);
    }

    @Test
    public void fetchToData_whenOnMsg_then_success() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(true, false, false);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg resultMsg = checkMsg(true);

        //check attributes
        checkAttributes(resultMsg, true, "cs_", clientAttributes);
        checkAttributes(resultMsg, true, "ss_", serverAttributes);
        checkAttributes(resultMsg, true, "shared_", sharedAttributes);

        //check timeseries
        checkTs(resultMsg, true, false, tsKeys);
    }

    @Test
    public void fetchToData_latestWithTs_whenOnMsg_then_success() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(true, true, false);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg resultMsg = checkMsg(true);

        //check attributes
        checkAttributes(resultMsg, true, "cs_", clientAttributes);
        checkAttributes(resultMsg, true, "ss_", serverAttributes);
        checkAttributes(resultMsg, true, "shared_", sharedAttributes);

        //check timeseries with ts
        checkTs(resultMsg, true, true, tsKeys);
    }

    @Test
    public void fetchToMetadata_whenOnMsg_then_failure() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(false, false, true);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg actualMsg = checkMsg(false);

        //check attributes
        checkAttributes(actualMsg, false, "cs_", clientAttributes);
        checkAttributes(actualMsg, false, "ss_", serverAttributes);
        checkAttributes(actualMsg, false, "shared_", sharedAttributes);

        //check timeseries with ts
        checkTs(actualMsg, false, false, tsKeys);
    }

    @Test
    public void fetchToData_whenOnMsg_then_failure() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(true, true, true);
        @NotNull TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        // check msg
        @NotNull TbMsg actualMsg = checkMsg(false);

        //check attributes
        checkAttributes(actualMsg, true, "cs_", clientAttributes);
        checkAttributes(actualMsg, true, "ss_", serverAttributes);
        checkAttributes(actualMsg, true, "shared_", sharedAttributes);

        //check timeseries with ts
        checkTs(actualMsg, true, true, tsKeys);
    }

    @Test
    public void fetchToData_whenOnMsg_and_data_is_not_object_then_failure() throws Exception {
        @NotNull TbGetAttributesNode node = initNode(true, true, true);
        @NotNull TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), "[]");
        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(IllegalArgumentException.class);
        Mockito.verify(ctx, never()).tellSuccess(any());
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        Assert.assertSame(msg, newMsgCaptor.getValue());
        Assert.assertNotNull(exceptionCaptor.getValue());
    }

    @NotNull
    private TbMsg checkMsg(boolean checkSuccess) {
        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        if (checkSuccess) {
            Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        } else {
            @NotNull ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
            Mockito.verify(ctx, never()).tellSuccess(any());
            Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(msgCaptor.capture(), exceptionCaptor.capture());
            RuntimeException exception = exceptionCaptor.getValue();
            Assert.assertNotNull(exception);
            Assert.assertNotNull(exception.getMessage());
            Assert.assertTrue(exception.getMessage().startsWith("The following attribute/telemetry keys is not present in the DB:"));
        }

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getMetaData());
        Assert.assertNotNull(resultMsg.getData());
        return resultMsg;
    }

    private void checkAttributes(@NotNull TbMsg actualMsg, boolean fetchToData, String prefix, @NotNull List<String> attributes) {
        JsonNode msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result;
                    if (fetchToData) {
                        result = msgData.get(prefix + attribute).asText();
                    } else {
                        result = actualMsg.getMetaData().getValue(prefix + attribute);
                    }
                    Assert.assertNotNull(result);
                    Assert.assertEquals(attribute + "_value", result);
                });
    }

    private void checkTs(@NotNull TbMsg actualMsg, boolean fetchToData, boolean getLatestValueWithTs, @NotNull List<String> tsKeys) {
        JsonNode msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        long value = 1L;
        for (@NotNull String key : tsKeys) {
            if (key.equals("unknown")) {
                continue;
            }
            @Nullable String actualValue;
            String expectedValue;
            if (getLatestValueWithTs) {
                expectedValue = "{\"ts\":" + ts + ",\"value\":{\"data\":" + value + "}}";
            } else {
                expectedValue = "{\"data\":" + value + "}";
            }
            if (fetchToData) {
                actualValue = JacksonUtil.toString(msgData.get(key));
            } else {
                actualValue = actualMsg.getMetaData().getValue(key);
            }
            Assert.assertNotNull(actualValue);
            Assert.assertEquals(expectedValue, actualValue);
            value++;
        }
    }

    @NotNull
    private TbGetAttributesNode initNode(boolean fetchToData, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
        @NotNull TbGetAttributesNodeConfiguration config = new TbGetAttributesNodeConfiguration();
        config.setClientAttributeNames(List.of("client_attr_1", "client_attr_2", "${client_attr_metadata}", "unknown"));
        config.setServerAttributeNames(List.of("server_attr_1", "server_attr_2", "${server_attr_metadata}", "unknown"));
        config.setSharedAttributeNames(List.of("shared_attr_1", "shared_attr_2", "$[shared_attr_data]", "unknown"));
        config.setLatestTsKeyNames(List.of("temperature", "humidity", "unknown"));
        config.setFetchToData(fetchToData);
        config.setGetLatestValueWithTs(getLatestValueWithTs);
        config.setTellFailureIfAbsent(isTellFailureIfAbsent);
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        @NotNull TbGetAttributesNode node = new TbGetAttributesNode();
        node.init(ctx, nodeConfiguration);
        return node;
    }

    @NotNull
    private TbMsg getTbMsg(EntityId entityId) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("shared_attr_data", "shared_attr_3");

        @NotNull TbMsgMetaData msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("client_attr_metadata", "client_attr_3");
        msgMetaData.putValue("server_attr_metadata", "server_attr_3");

        return TbMsg.newMsg("TEST", entityId, msgMetaData, JacksonUtil.toString(msgData));
    }

    @NotNull
    private List<String> getAttributeNames(String prefix) {
        return List.of(prefix + "_attr_1", prefix + "_attr_2", prefix + "_attr_3", "unknown");
    }

    @NotNull
    private List<AttributeKvEntry> getListAttributeKvEntry(@NotNull List<String> attributes, long ts) {
        return attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .map(attribute -> toAttributeKvEntry(ts, attribute))
                .collect(Collectors.toList());
    }

    @NotNull
    private BaseAttributeKvEntry toAttributeKvEntry(long ts, String attribute) {
        return new BaseAttributeKvEntry(ts, new StringDataEntry(attribute, attribute + "_value"));
    }

    @NotNull
    private List<TsKvEntry> getListTsKvEntry(@NotNull List<String> keys, long ts) {
        long value = 1L;
        @NotNull List<TsKvEntry> kvEntries = new ArrayList<>();
        for (@NotNull String key : keys) {
            if (key.equals("unknown")) {
                continue;
            }
            @NotNull String dataValue = "{\"data\":" + value + "}";
            kvEntries.add(new BasicTsKvEntry(ts, new JsonDataEntry(key, dataValue)));
            value++;
        }
        return kvEntries;
    }

}
