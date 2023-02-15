package org.thingsboard.server.common.stats;

public enum StatsType {
    RULE_ENGINE("ruleEngine"), CORE("core"), TRANSPORT("transport"), JS_INVOKE("jsInvoke"), RATE_EXECUTOR("rateExecutor");

    private String name;

    StatsType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
