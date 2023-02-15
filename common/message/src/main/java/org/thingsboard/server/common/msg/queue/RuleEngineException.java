package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleEngineException extends Exception {
    protected static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    private long ts;

    public RuleEngineException(String message) {
        super(message != null ? message : "Unknown");
        ts = System.currentTimeMillis();
    }

    public String toJsonString() {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode().put("message", getMessage()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }
}
