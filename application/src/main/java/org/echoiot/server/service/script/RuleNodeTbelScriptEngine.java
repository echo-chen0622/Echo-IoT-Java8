package org.echoiot.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.script.api.RuleNodeScriptFactory;
import org.echoiot.script.api.tbel.TbelInvokeService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
public class RuleNodeTbelScriptEngine extends RuleNodeScriptEngine<TbelInvokeService, Object> {

    public RuleNodeTbelScriptEngine(TenantId tenantId, TbelInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    @NotNull
    @Override
    protected ListenableFuture<Boolean> executeFilterTransform(Object result) {
        if (result instanceof Boolean) {
            return Futures.immediateFuture((Boolean) result);
        }
        return wrongResultType(result);
    }

    @NotNull
    @Override
    protected ListenableFuture<List<TbMsg>> executeUpdateTransform(@NotNull TbMsg msg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg((Map) result, msg)));
        } else if (result instanceof Collection) {
            @NotNull List<TbMsg> res = new ArrayList<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof Map) {
                    res.add(unbindMsg((Map) resObject, msg));
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    @NotNull
    @Override
    protected ListenableFuture<TbMsg> executeGenerateTransform(@NotNull TbMsg prevMsg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(unbindMsg((Map) result, prevMsg));
        }
        return wrongResultType(result);
    }

    @NotNull
    @Override
    protected ListenableFuture<String> executeToStringTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture((String) result);
        } else {
            return Futures.immediateFuture(JacksonUtil.toString(result));
        }
    }

    @NotNull
    @Override
    protected ListenableFuture<Set<String>> executeSwitchTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture(Collections.singleton((String) result));
        } else if (result instanceof Collection) {
            @NotNull Set<String> res = new HashSet<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof String) {
                    res.add((String) resObject);
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    @NotNull
    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(@NotNull TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), JacksonUtil::valueToTree, MoreExecutors.directExecutor());

    }

    @Override
    protected Object convertResult(Object result) {
        return result;
    }

    @NotNull
    @Override
    protected Object[] prepareArgs(@NotNull TbMsg msg) {
        @NotNull Object[] args = new Object[3];
        if (msg.getData() != null) {
            args[0] = JacksonUtil.fromString(msg.getData(), Object.class);
        } else {
            args[0] = new HashMap<>();
        }
        args[1] = new HashMap<>(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    @NotNull
    private static TbMsg unbindMsg(@NotNull Map msgData, @NotNull TbMsg msg) {
        @Nullable String data = null;
        @Nullable Map<String, String> metadata = null;
        @Nullable String messageType = null;
        if (msgData.containsKey(RuleNodeScriptFactory.MSG)) {
            data = JacksonUtil.toString(msgData.get(RuleNodeScriptFactory.MSG));
        }
        if (msgData.containsKey(RuleNodeScriptFactory.METADATA)) {
            Object msgMetadataObj = msgData.get(RuleNodeScriptFactory.METADATA);
            if (msgMetadataObj instanceof Map) {
                metadata = ((Map<?, ?>) msgMetadataObj).entrySet().stream().filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            } else {
                metadata = JacksonUtil.convertValue(msgMetadataObj, new TypeReference<>() {
                });
            }
        }
        if (msgData.containsKey(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).toString();
        }
        String newData = data != null ? data : msg.getData();
        @NotNull TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
        return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
    }

    @NotNull
    private static <T> ListenableFuture<T> wrongResultType(Object result) {
        @NotNull String className = toClassName(result);
        log.warn("Wrong result type: {}", className);
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + className));
    }

    @NotNull
    private static String toClassName(@Nullable Object result) {
        return result != null ? result.getClass().getSimpleName() : "null";
    }
}
