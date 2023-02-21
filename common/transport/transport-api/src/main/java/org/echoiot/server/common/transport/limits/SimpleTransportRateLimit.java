package org.echoiot.server.common.transport.limits;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.msg.tools.TbRateLimits;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SimpleTransportRateLimit implements TransportRateLimit {

    @NotNull
    private final TbRateLimits rateLimit;
    @Getter
    private final String configuration;

    public SimpleTransportRateLimit(@NotNull String configuration) {
        this.configuration = configuration;
        this.rateLimit = new TbRateLimits(configuration);
    }

    @Override
    public boolean tryConsume() {
        return rateLimit.tryConsume();
    }

    @Override
    public boolean tryConsume(long number) {
        return number <= 0 || rateLimit.tryConsume(number);
    }
}
