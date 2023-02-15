package org.thingsboard.server.queue.util;

import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FSTUtils;

import java.util.Optional;

@Slf4j
@Service
public class ProtoWithFSTService implements DataDecodingEncodingService {

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    @Override
    public <T> Optional<T> decode(byte[] byteArray) {
        try {
            return Optional.ofNullable(FSTUtils.decode(byteArray));
        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public <T> byte[] encode(T msq) {
        return FSTUtils.encode(msq);
    }


}
