package org.echoiot.server.common.msg.queue;

import org.jetbrains.annotations.NotNull;

public enum ServiceType {

    TB_CORE, TB_RULE_ENGINE, TB_TRANSPORT, JS_EXECUTOR, TB_VC_EXECUTOR;

    @NotNull
    public static ServiceType of(@NotNull String serviceType) {
        return ServiceType.valueOf(serviceType.replace("-", "_").toUpperCase());
    }
}
