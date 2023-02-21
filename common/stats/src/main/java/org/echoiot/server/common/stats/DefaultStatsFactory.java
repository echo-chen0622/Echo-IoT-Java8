package org.echoiot.server.common.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultStatsFactory implements StatsFactory {
    private static final String TOTAL_MSGS = "totalMsgs";
    private static final String SUCCESSFUL_MSGS = "successfulMsgs";
    private static final String FAILED_MSGS = "failedMsgs";

    private static final String STATS_NAME_TAG = "statsName";

    private static final Counter STUB_COUNTER = new StubCounter();

    @Resource
    private MeterRegistry meterRegistry;

    @Value("${metrics.enabled:false}")
    private Boolean metricsEnabled;

    @Value("${metrics.timer.percentiles:0.5}")
    private String timerPercentilesStr;

    private double[] timerPercentiles;

    @PostConstruct
    public void init() {
        if (!StringUtils.isEmpty(timerPercentilesStr)) {
            @NotNull String[] split = timerPercentilesStr.split(",");
            timerPercentiles = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                timerPercentiles[i] = Double.parseDouble(split[i]);
            }
        }
    }


    @NotNull
    @Override
    public StatsCounter createStatsCounter(@NotNull String key, String statsName) {
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, STATS_NAME_TAG, statsName)
                        : STUB_COUNTER,
                statsName
        );
    }

    @NotNull
    @Override
    public DefaultCounter createDefaultCounter(@NotNull String key, String... tags) {
        return new DefaultCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, tags)
                        : STUB_COUNTER
        );
    }

    @Nullable
    @Override
    public <T extends Number> T createGauge(@NotNull String key, @NotNull T number, String... tags) {
        return meterRegistry.gauge(key, Tags.of(tags), number);
    }

    @NotNull
    @Override
    public MessagesStats createMessagesStats(@NotNull String key) {
        @NotNull StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS);
        @NotNull StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS);
        @NotNull StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    @NotNull
    @Override
    public Timer createTimer(@NotNull String key, String... tags) {
        @NotNull Timer.Builder timerBuilder = Timer.builder(key)
                                                   .tags(tags)
                                                   .publishPercentiles();
        if (timerPercentiles != null && timerPercentiles.length > 0) {
            timerBuilder.publishPercentiles(timerPercentiles);
        }
        return timerBuilder.register(meterRegistry);
    }

    private static class StubCounter implements Counter {
        @Override
        public void increment(double amount) {
        }

        @Override
        public double count() {
            return 0;
        }

        @Nullable
        @Override
        public Id getId() {
            return null;
        }
    }
}
