package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

@Data
public class TbJsSwitchNodeConfiguration implements NodeConfiguration<TbJsSwitchNodeConfiguration> {

    private static final String DEFAULT_JS_SCRIPT = "function nextRelation(metadata, msg) {\n" +
            "    return ['one','nine'];\n" +
            "}\n" +
            "if(msgType === 'POST_TELEMETRY_REQUEST') {\n" +
            "    return ['two'];\n" +
            "}\n" +
            "return nextRelation(metadata, msg);";

    private static final String DEFAULT_TBEL_SCRIPT = "function nextRelation(metadata, msg) {\n" +
            "    return ['one','nine'];\n" +
            "}\n" +
            "if(msgType == 'POST_TELEMETRY_REQUEST') {\n" +
            "    return ['two'];\n" +
            "}\n" +
            "return nextRelation(metadata, msg);";

    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @NotNull
    @Override
    public TbJsSwitchNodeConfiguration defaultConfiguration() {
        @NotNull TbJsSwitchNodeConfiguration configuration = new TbJsSwitchNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript(DEFAULT_JS_SCRIPT);
        configuration.setTbelScript(DEFAULT_TBEL_SCRIPT);
        return configuration;
    }
}
