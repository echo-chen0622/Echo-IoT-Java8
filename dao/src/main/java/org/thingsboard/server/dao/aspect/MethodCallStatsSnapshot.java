package org.thingsboard.server.dao.aspect;

import lombok.Data;

@Data
public class MethodCallStatsSnapshot {
    private final int executions;
    private final int failures;
    private final long timing;
}
