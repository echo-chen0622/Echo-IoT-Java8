package org.echoiot.rest.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.echoiot.server.common.data.kv.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestJsonConverter {
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String LAST_UPDATE_TS = "lastUpdateTs";
    private static final String TS = "ts";

    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";

    @NotNull
    public static List<AttributeKvEntry> toAttributes(@NotNull List<JsonNode> attributes) {
        if (!CollectionUtils.isEmpty(attributes)) {
            return attributes.stream().map(attr -> {
                        @NotNull KvEntry entry = parseValue(attr.get(KEY).asText(), attr.get(VALUE));
                        return new BaseAttributeKvEntry(entry, attr.get(LAST_UPDATE_TS).asLong());
                    }
            ).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull
    public static List<TsKvEntry> toTimeseries(@NotNull Map<String, List<JsonNode>> timeseries) {
        if (!CollectionUtils.isEmpty(timeseries)) {
            @NotNull List<TsKvEntry> result = new ArrayList<>();
            timeseries.forEach((key, values) ->
                    result.addAll(values.stream().map(ts -> {
                                @NotNull KvEntry entry = parseValue(key, ts.get(VALUE));
                                return new BasicTsKvEntry(ts.get(TS).asLong(), entry);
                            }
                    ).collect(Collectors.toList()))
            );
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull
    private static KvEntry parseValue(String key, @NotNull JsonNode value) {
        if (!value.isContainerNode()) {
            if (value.isBoolean()) {
                return new BooleanDataEntry(key, value.asBoolean());
            } else if (value.isNumber()) {
                return parseNumericValue(key, value);
            } else if (value.isTextual()) {
                return new StringDataEntry(key, value.asText());
            } else {
                throw new RuntimeException(CAN_T_PARSE_VALUE + value);
            }
        } else {
            return new JsonDataEntry(key, value.toString());
        }
    }

    @NotNull
    private static KvEntry parseNumericValue(String key, @NotNull JsonNode value) {
        if (value.isFloatingPointNumber()) {
            return new DoubleDataEntry(key, value.asDouble());
        } else {
            try {
                long longValue = Long.parseLong(value.toString());
                return new LongDataEntry(key, longValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Big integer values are not supported!");
            }
        }
    }
}
