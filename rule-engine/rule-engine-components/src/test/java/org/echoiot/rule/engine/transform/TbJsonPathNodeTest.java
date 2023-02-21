package org.echoiot.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.PathNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TbJsonPathNodeTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbJsonPathNode node;
    TbJsonPathNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbJsonPathNodeConfiguration();
        config.setJsonPath("$.Attribute_2");
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbJsonPathNode());
        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenFail() {
        config.setJsonPath("");
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctx, nodeConfiguration)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        @NotNull TbJsonPathNodeConfiguration defaultConfig = new TbJsonPathNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getJsonPath()).isEqualTo(TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH);
    }

    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyOutputJsonPrimitiveNode() throws Exception {
        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_2\":100}";
        VerifyOutputMsg(data, 1, 100);

        data = "{\"Attribute_1\":22.5,\"Attribute_2\":\"StringValue\"}";
        VerifyOutputMsg(data, 2, "StringValue");
    }

    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyJavaPrimitiveOutput() throws Exception {
        config.setJsonPath("$.attributes.length()");
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        @NotNull String data = "{\"attributes\":[{\"attribute_1\":10},{\"attribute_2\":20},{\"attribute_3\":30},{\"attribute_4\":40}]}";
        VerifyOutputMsg(data, 1, 4);

    }

    @Test
    void givenJsonArray_whenOnMsg_thenVerifyOutput() throws Exception {
        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"Attribute_3\":22.5,\"Attribute_4\":10.3}, {\"Attribute_5\":22.5,\"Attribute_6\":10.3}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    @Test
    void givenJsonNode_whenOnMsg_thenVerifyOutput() throws Exception {
        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_2\":{\"Attribute_3\":22.5,\"Attribute_4\":10.3}}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    @Test
    void givenJsonArrayWithFilter_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setJsonPath("$.Attribute_2[?(@.voltage > 200)]");
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"voltage\":220}, {\"voltage\":250}, {\"voltage\":110}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode("[{\"voltage\":220}, {\"voltage\":250}]"));
    }

    @Test
    void givenNoArrayMsg_whenOnMsg_thenTellFailure() throws Exception {
        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        @NotNull TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenNoResultsForPath_whenOnMsg_thenTellFailure() throws Exception {
        @NotNull String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        @NotNull TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(PathNotFoundException.class);
    }

    private void VerifyOutputMsg(String data, int countTellSuccess, Object value) throws Exception {
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        node.onMsg(ctx, getTbMsg(deviceId, dataNode.toString()));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(countTellSuccess)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        assertThat(newMsgCaptor.getValue().getData()).isEqualTo(JacksonUtil.toString(value));
    }

    @NotNull
    private TbMsg getTbMsg(EntityId entityId, String data) {
        @NotNull Map<String, String> mdMap = Map.of("country", "US",
                                                    "city", "NY"
                                                   );
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(mdMap), data, callback);
    }
}
