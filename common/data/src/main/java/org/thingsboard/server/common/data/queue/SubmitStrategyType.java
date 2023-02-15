package org.thingsboard.server.common.data.queue;

public enum SubmitStrategyType {
    BURST, BATCH, SEQUENTIAL_BY_ORIGINATOR, SEQUENTIAL_BY_TENANT, SEQUENTIAL
}
