package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Timer;

public interface StatsFactory {
    StatsCounter createStatsCounter(String key, String statsName);

    DefaultCounter createDefaultCounter(String key, String... tags);

    <T extends Number> T createGauge(String key, T number, String... tags);

    MessagesStats createMessagesStats(String key);

    Timer createTimer(String key, String... tags);
}
