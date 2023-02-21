package org.echoiot.server.service.stats;

import org.echoiot.server.actors.JsInvokeStats;
import org.echoiot.server.common.stats.StatsCounter;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.common.stats.StatsType;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class DefaultJsInvokeStats implements JsInvokeStats {
    private static final String REQUESTS = "requests";
    private static final String RESPONSES = "responses";
    private static final String FAILURES = "failures";

    private StatsCounter requestsCounter;
    private StatsCounter responsesCounter;
    private StatsCounter failuresCounter;

    @Resource
    private StatsFactory statsFactory;

    @PostConstruct
    public void init() {
        String key = StatsType.JS_INVOKE.getName();
        this.requestsCounter = statsFactory.createStatsCounter(key, REQUESTS);
        this.responsesCounter = statsFactory.createStatsCounter(key, RESPONSES);
        this.failuresCounter = statsFactory.createStatsCounter(key, FAILURES);
    }

    @Override
    public void incrementRequests(int amount) {
        requestsCounter.add(amount);
    }

    @Override
    public void incrementResponses(int amount) {
        responsesCounter.add(amount);
    }

    @Override
    public void incrementFailures(int amount) {
        failuresCounter.add(amount);
    }

    @Override
    public int getRequests() {
        return requestsCounter.get();
    }

    @Override
    public int getResponses() {
        return responsesCounter.get();
    }

    @Override
    public int getFailures() {
        return failuresCounter.get();
    }

    @Override
    public void reset() {
        requestsCounter.clear();
        responsesCounter.clear();
        failuresCounter.clear();
    }
}
