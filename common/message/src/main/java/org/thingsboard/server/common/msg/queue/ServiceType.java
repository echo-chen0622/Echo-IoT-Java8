package org.thingsboard.server.common.msg.queue;

public enum ServiceType {

    TB_CORE, TB_RULE_ENGINE, TB_TRANSPORT, JS_EXECUTOR, TB_VC_EXECUTOR;

    public static ServiceType of(String serviceType) {
        return ServiceType.valueOf(serviceType.replace("-", "_").toUpperCase());
    }
}
