package org.thingsboard.server.common.data;

import lombok.Data;

@Data
public class ApiUsageStateMailMessage {
    private final ApiUsageRecordKey key;
    private final long threshold;
    private final long value;
}
