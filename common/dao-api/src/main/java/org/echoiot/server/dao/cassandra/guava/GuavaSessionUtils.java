package org.echoiot.server.dao.cassandra.guava;

import org.jetbrains.annotations.NotNull;

public class GuavaSessionUtils {
    @NotNull
    public static GuavaSessionBuilder builder() {
        return new GuavaSessionBuilder();
    }
}
