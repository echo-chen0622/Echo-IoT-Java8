package org.echoiot.server.dao.timeseries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum TsInsertExecutorType {
    SINGLE,
    FIXED,
    CACHED;

    @NotNull
    public static Optional<TsInsertExecutorType> parse(@Nullable String name) {
        @Nullable TsInsertExecutorType executorType = null;
        if (name != null) {
            for (@NotNull TsInsertExecutorType type : TsInsertExecutorType.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    executorType = type;
                    break;
                }
            }
        }
        return Optional.of(executorType);
    }
}
