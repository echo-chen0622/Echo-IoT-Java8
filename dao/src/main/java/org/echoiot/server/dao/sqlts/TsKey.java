package org.echoiot.server.dao.sqlts;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
public class TsKey {
    @NotNull
    private final UUID entityId;
    private final int key;
}
