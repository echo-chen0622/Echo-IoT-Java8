package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

@Data
public class TbClearAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration<TbClearAlarmNodeConfiguration> {

    @NotNull
    @Override
    public TbClearAlarmNodeConfiguration defaultConfiguration() {
        @NotNull TbClearAlarmNodeConfiguration configuration = new TbClearAlarmNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        configuration.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
        configuration.setAlarmType("General Alarm");
        return configuration;
    }
}
