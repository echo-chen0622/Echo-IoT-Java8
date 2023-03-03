package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.echoiot.server.common.data.validation.NoXss;

import java.util.Collections;
import java.util.List;

@Data
public class TbCreateAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration {

    @NoXss
    private String severity;
    private boolean propagate;
    private boolean propagateToOwner;
    private boolean propagateToTenant;
    private boolean useMessageAlarmData;
    private boolean overwriteAlarmDetails = true;
    private boolean dynamicSeverity;

    private List<String> relationTypes;

    @Override
    public TbCreateAlarmNodeConfiguration defaultConfiguration() {
        TbCreateAlarmNodeConfiguration configuration = new TbCreateAlarmNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        configuration.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
        configuration.setAlarmType("General Alarm");
        configuration.setSeverity(AlarmSeverity.CRITICAL.name());
        configuration.setPropagate(false);
        configuration.setPropagateToOwner(false);
        configuration.setPropagateToTenant(false);
        configuration.setUseMessageAlarmData(false);
        configuration.setOverwriteAlarmDetails(false);
        configuration.setRelationTypes(Collections.emptyList());
        configuration.setDynamicSeverity(false);
        return configuration;
    }

}
