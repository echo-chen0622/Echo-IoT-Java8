package org.echoiot.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TbDeleteKeysNodeTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbDeleteKeysNode node;
    TbDeleteKeysNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        config.setKeys(Set.of("TestKey_1", "TestKey_2", "TestKey_3", "(\\w*)Data(\\w*)"));
        config.setFromMetadata(true);
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbDeleteKeysNode());
        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        @NotNull TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptySet());
        assertThat(defaultConfig.isFromMetadata()).isEqualTo(false);
    }

    @Test
    void givenMsgFromMetadata_whenOnMsg_thenVerifyOutput() throws Exception {
        @NotNull String data = "{}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        Map<String, String> metaDataMap = newMsg.getMetaData().getData();
        assertThat(metaDataMap.containsKey("TestKey_1")).isEqualTo(false);
        assertThat(metaDataMap.containsKey("voltageDataValue")).isEqualTo(false);
    }

    @Test
    void givenMsgFromMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setFromMetadata(false);
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        @NotNull String data = "{\"Voltage\":22.5,\"TempDataValue\":10.5}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        JsonNode dataNode = JacksonUtil.toJsonNode(newMsg.getData());
        assertThat(dataNode.has("TempDataValue")).isEqualTo(false);
        assertThat(dataNode.has("Voltage")).isEqualTo(true);
    }

    @Test
    void givenEmptyKeys_whenOnMsg_thenVerifyOutput() throws Exception {
        @NotNull TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(defaultConfig));
        node.init(ctx, nodeConfiguration);

        @NotNull String data = "{\"Voltage\":220,\"Humidity\":56}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getData()).isEqualTo(data);
    }

    @NotNull
    private TbMsg getTbMsg(EntityId entityId, String data) {
        @NotNull final Map<String, String> mdMap = Map.of(
                "TestKey_1", "Test",
                "country", "US",
                "voltageDataValue", "220",
                "city", "NY"
                                                         );
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(mdMap), data, callback);
    }

}
