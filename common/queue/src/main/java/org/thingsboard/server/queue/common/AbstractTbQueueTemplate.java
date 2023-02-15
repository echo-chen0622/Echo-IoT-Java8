package org.thingsboard.server.queue.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AbstractTbQueueTemplate {
    protected static final String REQUEST_ID_HEADER = "requestId";
    protected static final String RESPONSE_TOPIC_HEADER = "responseTopic";
    protected static final String EXPIRE_TS_HEADER = "expireTs";

    protected byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    protected static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    protected byte[] stringToBytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    protected String bytesToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    protected static byte[] longToBytes(long x) {
        ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
        longBuffer.putLong(0, x);
        return longBuffer.array();
    }

    protected static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}
