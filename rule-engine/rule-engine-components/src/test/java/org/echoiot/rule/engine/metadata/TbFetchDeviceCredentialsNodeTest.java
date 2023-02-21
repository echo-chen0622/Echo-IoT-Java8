package org.echoiot.rule.engine.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.echoiot.server.common.data.security.DeviceCredentialsType.ACCESS_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

public class TbFetchDeviceCredentialsNodeTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbFetchDeviceCredentialsNode node;
    TbFetchDeviceCredentialsNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;
    DeviceCredentialsService deviceCredentialsService;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        config.setFetchToMetadata(true);
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbFetchDeviceCredentialsNode());
        node.init(ctx, nodeConfiguration);
        deviceCredentialsService = mock(DeviceCredentialsService.class);

        willReturn(deviceCredentialsService).given(ctx).getDeviceCredentialsService();
        willAnswer(invocation -> {
            @NotNull DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setCredentialsType(ACCESS_TOKEN);
            return deviceCredentials;
        }).given(deviceCredentialsService).findDeviceCredentialsByDeviceId(any(), any());
        willAnswer(invocation -> {
            return JacksonUtil.newObjectNode();
        }).given(deviceCredentialsService).toCredentialsInfo(any());
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
        assertThat(node.fetchToMetadata).isEqualTo(true);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        @NotNull TbFetchDeviceCredentialsNodeConfiguration defaultConfig = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.isFetchToMetadata()).isEqualTo(true);
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        node.onMsg(ctx, getTbMsg(deviceId));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getMetaData().getData().containsKey("credentials")).isEqualTo(true);
        assertThat(newMsg.getMetaData().getData().containsKey("credentialsType")).isEqualTo(true);
    }

    @Test
    void givenUnsupportedOriginatorType_whenOnMsg_thenTellFailure() throws Exception {
        node.onMsg(ctx, getTbMsg(new CustomerId(UUID.randomUUID())));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenGetDeviceCredentials_whenOnMsg_thenTellFailure() throws Exception {
        willAnswer(invocation -> {
            return null;
        }).given(deviceCredentialsService).findDeviceCredentialsByDeviceId(any(), any());

        node.onMsg(ctx, getTbMsg(deviceId));

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @NotNull
    private TbMsg getTbMsg(EntityId entityId) {
        @NotNull final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
                                                         );

        @NotNull final TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        @NotNull final String data = "{\"TestAttribute_1\": \"humidity\", \"TestAttribute_2\": \"voltage\"}";

        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, metaData, data, callback);
    }
}
