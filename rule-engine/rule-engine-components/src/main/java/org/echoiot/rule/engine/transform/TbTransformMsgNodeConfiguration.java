package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

@Data
public class TbTransformMsgNodeConfiguration extends TbTransformNodeConfiguration implements NodeConfiguration {

    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @NotNull
    @Override
    public TbTransformMsgNodeConfiguration defaultConfiguration() {
        @NotNull TbTransformMsgNodeConfiguration configuration = new TbTransformMsgNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript("return {msg: msg, metadata: metadata, msgType: msgType};");
        configuration.setTbelScript("return {msg: msg, metadata: metadata, msgType: msgType};");
        return configuration;
    }
}
