package org.thingsboard.server.common.data;

import lombok.Getter;

public enum ApiFeature {
    TRANSPORT("transportApiState", "Device API"),
    DB("dbApiState", "Telemetry persistence"),
    RE("ruleEngineApiState", "Rule Engine execution"),
    JS("jsExecutionApiState", "JavaScript functions execution"),
    EMAIL("emailApiState", "Email messages"),
    SMS("smsApiState", "SMS messages"),
    ALARM("alarmApiState", "Created alarms");

    @Getter
    private final String apiStateKey;
    @Getter
    private final String label;

    ApiFeature(String apiStateKey, String label) {
        this.apiStateKey = apiStateKey;
        this.label = label;
    }

}
