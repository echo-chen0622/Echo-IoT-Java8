package org.thingsboard.server.common.msg.tools;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.Getter;

import java.time.Duration;

/**
 * Created by ashvayka on 22.10.18.
 */
public class TbRateLimits {
    private final LocalBucket bucket;

    @Getter
    private final String configuration;

    public TbRateLimits(String limitsConfiguration) {
        this(limitsConfiguration, false);
    }

    public TbRateLimits(String limitsConfiguration, boolean refillIntervally) {
        LocalBucketBuilder builder = Bucket4j.builder();
        boolean initialized = false;
        for (String limitSrc : limitsConfiguration.split(",")) {
            long capacity = Long.parseLong(limitSrc.split(":")[0]);
            long duration = Long.parseLong(limitSrc.split(":")[1]);
            Refill refill = refillIntervally ? Refill.intervally(capacity, Duration.ofSeconds(duration)) : Refill.greedy(capacity, Duration.ofSeconds(duration));
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
