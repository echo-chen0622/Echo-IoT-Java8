package org.echoiot.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.rule.engine.api.ScriptEngine;
import org.echoiot.script.api.ScriptInvokeService;
import org.echoiot.script.api.ScriptType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
public abstract class RuleNodeScriptEngine<T extends ScriptInvokeService, R> implements ScriptEngine {

    private final T scriptInvokeService;

    private final UUID scriptId;
    private final TenantId tenantId;

    public RuleNodeScriptEngine(TenantId tenantId, T scriptInvokeService, String script, String... argNames) {
        this.tenantId = tenantId;
        this.scriptInvokeService = scriptInvokeService;
        try {
            this.scriptId = this.scriptInvokeService.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
        }
    }

    protected abstract Object[] prepareArgs(TbMsg msg);

    @NotNull
    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(@NotNull TbMsg msg) {
        ListenableFuture<R> result = executeScriptAsync(msg);
        return Futures.transformAsync(result,
                json -> executeUpdateTransform(msg, json),
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, R result);

    @NotNull
    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(@NotNull TbMsg prevMsg) {
        return Futures.transformAsync(executeScriptAsync(prevMsg),
                result -> executeGenerateTransform(prevMsg, result),
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, R result);

    @NotNull
    @Override
    public ListenableFuture<String> executeToStringAsync(@NotNull TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg), this::executeToStringTransform, MoreExecutors.directExecutor());
    }


    @NotNull
    @Override
    public ListenableFuture<Boolean> executeFilterAsync(@NotNull TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeFilterTransform,
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<String> executeToStringTransform(R result);

    protected abstract ListenableFuture<Boolean> executeFilterTransform(R result);

    protected abstract ListenableFuture<Set<String>> executeSwitchTransform(R result);

    @NotNull
    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(@NotNull TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeSwitchTransform,
                MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    ListenableFuture<R> executeScriptAsync(@NotNull TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        Object[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    @NotNull
    ListenableFuture<R> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transformAsync(scriptInvokeService.invokeScript(tenantId, customerId, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(convertResult(o));
                    } catch (Exception e) {
                        if (e.getCause() instanceof ScriptException) {
                            return Futures.immediateFailedFuture(e.getCause());
                        } else if (e.getCause() instanceof RuntimeException) {
                            return Futures.immediateFailedFuture(new ScriptException(e.getCause().getMessage()));
                        } else {
                            return Futures.immediateFailedFuture(new ScriptException(e));
                        }
                    }
                }, MoreExecutors.directExecutor());
    }

    public void destroy() {
        scriptInvokeService.release(this.scriptId);
    }

    protected abstract R convertResult(Object result);
}
