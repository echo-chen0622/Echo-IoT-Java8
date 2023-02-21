package org.echoiot.server.common.data;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class ApiUsageStateMailMessage {
    @NotNull
    private final ApiUsageRecordKey key;
    private final long threshold;
    private final long value;
}
