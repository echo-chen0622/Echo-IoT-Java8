package org.echoiot.rule.engine.debug;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgGeneratorNodeConfiguration implements NodeConfiguration<TbMsgGeneratorNodeConfiguration> {

    public static final int UNLIMITED_MSG_COUNT = 0;
    public static final String DEFAULT_SCRIPT = "var msg = { temp: 42, humidity: 77 };\n" +
            "var metadata = { data: 40 };\n" +
            "var msgType = \"POST_TELEMETRY_REQUEST\";\n\n" +
            "return { msg: msg, metadata: metadata, msgType: msgType };";

    private int msgCount;
    private int periodInSeconds;
    private String originatorId;
    private EntityType originatorType;
    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @NotNull
    @Override
    public TbMsgGeneratorNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgGeneratorNodeConfiguration configuration = new TbMsgGeneratorNodeConfiguration();
        configuration.setMsgCount(UNLIMITED_MSG_COUNT);
        configuration.setPeriodInSeconds(1);
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript(DEFAULT_SCRIPT);
        configuration.setTbelScript(DEFAULT_SCRIPT);
        return configuration;
    }
}
