package org.echoiot.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.script.api.RuleNodeScriptFactory;
import org.echoiot.script.api.js.JsInvokeService;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public class RuleNodeJsScriptEngine extends RuleNodeScriptEngine<JsInvokeService, JsonNode> {

    public RuleNodeJsScriptEngine(TenantId tenantId, JsInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return executeScriptAsync(msg);
    }

    @Override
    protected ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, JsonNode json) {
        if (json.isObject()) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg(json, msg)));
        } else if (json.isArray()) {
            List<TbMsg> res = new ArrayList<>(json.size());
            json.forEach(jsonObject -> res.add(unbindMsg(jsonObject, msg)));
            return Futures.immediateFuture(res);
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    @Override
    protected ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, JsonNode result) {
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
        }
        return Futures.immediateFuture(unbindMsg(result, prevMsg));
    }

    @Override
    protected JsonNode convertResult(Object result) {
        return JacksonUtil.toJsonNode(result != null ? result.toString() : null);
    }

    @Override
    protected ListenableFuture<String> executeToStringTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(result.asText());
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    @Override
    protected ListenableFuture<Boolean> executeFilterTransform(JsonNode json) {
        if (json.isBoolean()) {
            return Futures.immediateFuture(json.asBoolean());
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    @Override
    protected ListenableFuture<Set<String>> executeSwitchTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(Collections.singleton(result.asText()));
        }
        if (result.isArray()) {
            Set<String> nextStates = new HashSet<>();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    log.warn("Wrong result type: {}", val.getNodeType());
                    return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + val.getNodeType()));
                } else {
                    nextStates.add(val.asText());
                }
            }
            return Futures.immediateFuture(nextStates);
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        String[] args = new String[3];
        if (msg.getData() != null) {
            args[0] = msg.getData();
        } else {
            args[0] = "";
        }
        args[1] = JacksonUtil.toString(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    private static TbMsg unbindMsg(JsonNode msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.has(RuleNodeScriptFactory.MSG)) {
            JsonNode msgPayload = msgData.get(RuleNodeScriptFactory.MSG);
            data = JacksonUtil.toString(msgPayload);
        }
        if (msgData.has(RuleNodeScriptFactory.METADATA)) {
            JsonNode msgMetadata = msgData.get(RuleNodeScriptFactory.METADATA);
            metadata = JacksonUtil.convertValue(msgMetadata, new TypeReference<>() {
            });
        }
        if (msgData.has(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).asText();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
        return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
    }
}