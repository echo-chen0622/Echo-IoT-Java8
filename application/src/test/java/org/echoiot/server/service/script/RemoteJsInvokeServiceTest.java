package org.echoiot.server.service.script;

import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.StringUtils;
import org.echoiot.script.api.ScriptType;
import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.common.stats.TbApiUsageStateClient;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.echoiot.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.echoiot.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.echoiot.server.queue.TbQueueRequestTemplate;
import org.echoiot.server.queue.common.TbProtoJsQueueMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RemoteJsInvokeServiceTest {

    private RemoteJsInvokeService remoteJsInvokeService;
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> jsRequestTemplate;


    @BeforeEach
    public void beforeEach() {
        TbApiUsageStateClient apiUsageStateClient = mock(TbApiUsageStateClient.class);
        ApiUsageState apiUsageState = mock(ApiUsageState.class);
        when(apiUsageState.isJsExecEnabled()).thenReturn(true);
        when(apiUsageStateClient.getApiUsageState(any())).thenReturn(apiUsageState);
        TbApiUsageReportClient apiUsageReportClient = mock(TbApiUsageReportClient.class);

        remoteJsInvokeService = new RemoteJsInvokeService(Optional.of(apiUsageStateClient), Optional.of(apiUsageReportClient));
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
    }

    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    @Test
    public void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        mockJsEvalResponse();
        @NotNull String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        @NotNull String expectedInvocationResult = "scriptInvocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        @NotNull ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        @NotNull JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(jsInvokeRequestMade.getScriptHash()).isEqualTo(getScriptHash(scriptId));
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        mockJsEvalResponse();
        @NotNull String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorCode(JsInvokeProtos.JsInvokeErrorCode.NOT_FOUND_ERROR)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        @NotNull String expectedInvocationResult = "invocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isNotEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        @NotNull ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestsCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate, times(2)).send(jsRequestsCaptor.capture());

        List<TbProtoJsQueueMsg<RemoteJsRequest>> jsInvokeRequestsMade = jsRequestsCaptor.getAllValues();

        @NotNull JsInvokeProtos.JsInvokeRequest firstRequestMade = jsInvokeRequestsMade.get(0).getValue().getInvokeRequest();
        assertThat(firstRequestMade.getScriptBody()).isNullOrEmpty();

        @NotNull JsInvokeProtos.JsInvokeRequest secondRequestMade = jsInvokeRequestsMade.get(1).getValue().getInvokeRequest();
        assertThat(secondRequestMade.getScriptBody()).contains(scriptBody);

        assertThat(jsInvokeRequestsMade.stream().map(TbProtoQueueMsg::getKey).distinct().count()).as("partition keys are same")
                .isOne();

        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody() throws Exception {
        mockJsEvalResponse();

        @NotNull TenantId tenantId1 = TenantId.fromUUID(UUID.randomUUID());
        @NotNull String scriptBody1 = "var msg = { temp: 42, humidity: 77 };\n" +
                                      "var metadata = { data: 40 };\n" +
                                      "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                                      "\n" +
                                      "return { msg: msg, metadata: metadata, msgType: msgType };";

        @NotNull Set<String> scriptHashes = new HashSet<>();
        @Nullable String tenant1Script1Hash = null;
        for (int i = 0; i < 3; i++) {
            UUID scriptUuid = remoteJsInvokeService.eval(tenantId1, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
            tenant1Script1Hash = getScriptHash(scriptUuid);
            scriptHashes.add(tenant1Script1Hash);
        }
        assertThat(scriptHashes).as("Unique scripts ids").size().isOne();

        @NotNull TenantId tenantId2 = TenantId.fromUUID(UUID.randomUUID());
        UUID scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
        String tenant2Script1Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script1Id).isNotEqualTo(tenant1Script1Hash);

        @NotNull String scriptBody2 = scriptBody1 + ";;";
        scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody2).get();
        String tenant2Script2Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script2Id).isNotEqualTo(tenant2Script1Id);
    }

    @Test
    public void whenReleasingScript_thenCheckForHashUsages() throws Exception {
        mockJsEvalResponse();
        @NotNull String scriptBody = "return { a: 'b'};";
        UUID scriptId1 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        UUID scriptId2 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        String scriptHash = getScriptHash(scriptId1);
        assertThat(scriptHash).isEqualTo(getScriptHash(scriptId2));
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setReleaseResponse(JsInvokeProtos.JsReleaseResponse.newBuilder()
                        .setSuccess(true)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        remoteJsInvokeService.release(scriptId1).get();
        verifyNoInteractions(jsRequestTemplate);
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).containsKey(scriptHash);

        remoteJsInvokeService.release(scriptId2).get();
        verify(jsRequestTemplate).send(any());
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).isEmpty();
    }

    private String getScriptHash(UUID scriptUuid) {
        return remoteJsInvokeService.getScriptHash(scriptUuid);
    }

    private void mockJsEvalResponse() {
        doAnswer(methodCall -> Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setCompileResponse(JsInvokeProtos.JsCompileResponse.newBuilder()
                        .setSuccess(true)
                        .setScriptHash(methodCall.<TbProtoQueueMsg<RemoteJsRequest>>getArgument(0).getValue().getCompileRequest().getScriptHash())
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> jsQueueMsg.getValue().hasCompileRequest()));
    }

}
