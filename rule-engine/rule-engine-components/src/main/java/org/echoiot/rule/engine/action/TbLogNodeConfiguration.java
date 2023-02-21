package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

@Data
public class TbLogNodeConfiguration implements NodeConfiguration {

    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @NotNull
    @Override
    public TbLogNodeConfiguration defaultConfiguration() {
        @NotNull TbLogNodeConfiguration configuration = new TbLogNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript("return '\\nIncoming message:\\n' + JSON.stringify(msg) + '\\nIncoming metadata:\\n' + JSON.stringify(metadata);");
        configuration.setTbelScript("return '\\nIncoming message:\\n' + JSON.stringify(msg) + '\\nIncoming metadata:\\n' + JSON.stringify(metadata);");
        return configuration;
    }
}
