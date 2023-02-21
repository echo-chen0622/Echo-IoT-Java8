package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

import static org.echoiot.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "script", relationTypes = {"True", "False"},
        configClazz = TbJsFilterNodeConfiguration.class,
        nodeDescription = "Filter incoming messages using TBEL or JS script",
        nodeDetails = "Evaluates boolean function using incoming message. " +
                "The function may be written using TBEL or plain JavaScript. " +
                "Script function should return boolean value and accepts three parameters: <br/>" +
                "Message payload can be accessed via <code>msg</code> property. For example <code>msg.temperature < 10;</code><br/>" +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>metadata.customerName === 'John';</code><br/>" +
                "Message type can be accessed via <code>msgType</code> property.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeScriptConfig"
)
public class TbJsFilterNode implements TbNode {

    private TbJsFilterNodeConfiguration config;
    private ScriptEngine scriptEngine;

    @Override
    public void init(@NotNull TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsFilterNodeConfiguration.class);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        withCallback(scriptEngine.executeFilterAsync(msg),
                filterResult -> {
                    ctx.logJsEvalResponse();
                    ctx.tellNext(msg, filterResult ? "True" : "False");
                },
                t -> {
                    ctx.tellFailure(msg, t);
                    ctx.logJsEvalFailure();
                }, ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }
}
