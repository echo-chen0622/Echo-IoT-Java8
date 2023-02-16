package org.echoiot.server.queue.util;

import java.util.Optional;

public interface DataDecodingEncodingService {

    <T> Optional<T> decode(byte[] byteArray);

    <T> byte[] encode(T msq);

}
