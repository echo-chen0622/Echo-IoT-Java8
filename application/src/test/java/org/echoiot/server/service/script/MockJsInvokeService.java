package org.echoiot.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.echoiot.script.api.ScriptType;
import org.echoiot.script.api.js.JsInvokeService;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "mock")
public class MockJsInvokeService implements JsInvokeService {

    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        log.warn("eval {} {} {} {}", tenantId, scriptType, scriptBody, argNames);
        return Futures.immediateFuture(UUID.randomUUID());
    }

    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        log.warn("invokeFunction {} {} {} {}", tenantId, customerId, scriptId, args);
        return Futures.immediateFuture("{}");
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        log.warn("release {}", scriptId);
        return Futures.immediateFuture(null);
    }
}