package org.echoiot.common.util;

import org.echoiot.server.common.data.kv.KvEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KvUtil {

    @Nullable
    public static String getStringValue(@NotNull KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(String::valueOf).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(String::valueOf).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(String::valueOf).orElse(null);
            case STRING:
                return entry.getStrValue().orElse("");
            case JSON:
                return entry.getJsonValue().orElse("");
            default:
                return null;
        }
    }

    @Nullable
    public static Double getDoubleValue(@NotNull KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(Long::doubleValue).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(e -> e ? 1.0 : 0).orElse(null);
            case STRING:
                try {
                    return Double.parseDouble(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    @Nullable
    public static Boolean getBoolValue(@NotNull KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(e -> e != 0).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(e -> e != 0).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().orElse(null);
            case STRING:
                try {
                    return Boolean.parseBoolean(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

}
