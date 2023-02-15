package org.thingsboard.server.common.msg.tools;

import org.awaitility.pollinterval.FixedPollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class RateLimitsTest {

    @Test
    public void testRateLimits_greedyRefill() {
        testRateLimitWithGreedyRefill(3, 10);
        testRateLimitWithGreedyRefill(3, 3);
        testRateLimitWithGreedyRefill(4, 2);
    }

    private void testRateLimitWithGreedyRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = (int) (((double) period / capacity) * 1000);
        int gap = 500;

        for (int i = 0; i < capacity; i++) {
            await("token refill for rate limit " + rateLimitConfig)
                    .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                    .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                    .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    });
            assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
        }
    }

    @Test
    public void testRateLimits_intervalRefill() {
        testRateLimitWithIntervalRefill(10, 5);
        testRateLimitWithIntervalRefill(3, 3);
        testRateLimitWithIntervalRefill(4, 2);
    }

    private void testRateLimitWithIntervalRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig, true);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = period * 1000;
        int gap = 500;

        await("tokens refill for rate limit " + rateLimitConfig)
                .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    for (int i = 0; i < capacity; i++) {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    }
                    assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
                });
    }

}
