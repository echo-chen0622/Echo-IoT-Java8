package org.echoiot.server.common.transport.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.echoiot.server.gen.transport.TransportProtos.KeyValueProto;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JsonUtils {

    private static final JsonParser jsonParser = new JsonParser();
    private static final ObjectMapper json = new ObjectMapper();

    @NotNull
    public static JsonObject getJsonObject(@NotNull List<KeyValueProto> tsKv) {
        @NotNull JsonObject json = new JsonObject();
        for (@NotNull KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
                case JSON_V:
                    json.add(kv.getKey(), jsonParser.parse(kv.getJsonV()));
                    break;
            }
        }
        return json;
    }

    public static JsonElement parse(@NotNull String params) {
        return jsonParser.parse(params);
    }

}
