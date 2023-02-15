package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueRemoteJsInvokeSettings {
    @Value("${queue.js.request_topic}")
    private String requestTopic;

    @Value("${queue.js.response_topic_prefix}")
    private String responseTopic;

    @Value("${queue.js.max_pending_requests}")
    private long maxPendingRequests;

    @Value("${queue.js.response_poll_interval}")
    private int responsePollInterval;

    @Value("${queue.js.max_requests_timeout}")
    private long maxRequestsTimeout;
}
