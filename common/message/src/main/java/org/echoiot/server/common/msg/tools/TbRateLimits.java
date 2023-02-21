package org.echoiot.server.common.msg.tools;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Created by Echo on 22.10.18.
 */
public class TbRateLimits {
    private final LocalBucket bucket;

    @NotNull
    @Getter
    private final String configuration;

    public TbRateLimits(@NotNull String limitsConfiguration) {
        this(limitsConfiguration, false);
    }

    public TbRateLimits(@NotNull String limitsConfiguration, boolean refillIntervally) {
        @NotNull LocalBucketBuilder builder = Bucket4j.builder();
        boolean initialized = false;
        for (@NotNull String limitSrc : limitsConfiguration.split(",")) {
            long capacity = Long.parseLong(limitSrc.split(":")[0]);
            long duration = Long.parseLong(limitSrc.split(":")[1]);
            @NotNull Refill refill = refillIntervally ? Refill.intervally(capacity, Duration.ofSeconds(duration)) : Refill.greedy(capacity, Duration.ofSeconds(duration));
            builder.addLimit(Bandwidth.classic(capacity, refill));
            initialized = true;
        }
        if (initialized) {
            bucket = builder.build();
        } else {
            throw new IllegalArgumentException("Failed to parse rate limits configuration: " + limitsConfiguration);
        }
        this.configuration = limitsConfiguration;
    }

    public boolean tryConsume() {
        return bucket.tryConsume(1);
    }

    public boolean tryConsume(long number) {
        return bucket.tryConsume(number);
    }

}
