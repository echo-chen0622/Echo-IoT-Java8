package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "script",
        configClazz = TbTransformMsgNodeConfiguration.class,
        nodeDescription = "Change Message payload, Metadata or Message type using JavaScript",
        nodeDetails = "JavaScript function receive 3 input parameters <br/> " +
                "<code>metadata</code> - is a Message metadata.<br/>" +
                "<code>msg</code> - is a Message payload.<br/>" +
                "<code>msgType</code> - is a Message type.<br/>" +
                "Should return the following structure:<br/>" +
                "<code>{ msg: <i style=\"color: #666;\">new payload</i>,<br/>&nbsp&nbsp&nbspmetadata: <i style=\"color: #666;\">new metadata</i>,<br/>&nbsp&nbsp&nbspmsgType: <i style=\"color: #666;\">new msgType</i> }</code><br/>" +
                "All fields in resulting object are optional and will be taken from original message if not specified.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeScriptConfig"
)
public class TbTransformMsgNode extends TbAbstractTransformNode {

    private TbTransformMsgNodeConfiguration config;
    private ScriptEngine scriptEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTransformMsgNodeConfiguration.class);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        return scriptEngine.executeUpdateAsync(msg);
    }

    @Override
    protected void transformSuccess(TbContext ctx, TbMsg msg, TbMsg m) {
        ctx.logJsEvalResponse();
        super.transformSuccess(ctx, msg, m);
    }

    @Override
    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.logJsEvalFailure();
        super.transformFailure(ctx, msg, t);
    }

    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }
}
