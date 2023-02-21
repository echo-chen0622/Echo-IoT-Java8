package org.echoiot.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.echoiot.server.common.data.kv.DataType;
import org.echoiot.server.common.data.kv.KvEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Created by Valerii Sosliuk on 5/12/2017.
 */
public class JacksonUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final ObjectMapper PRETTY_SORTED_JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build();
    public static ObjectMapper ALLOW_UNQUOTED_FIELD_NAMES_MAPPER = JsonMapper.builder()
            .configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .build();

    @Nullable
    public static <T> T convertValue(@Nullable Object fromValue, Class<T> toValueType) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueType) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value: "
                    + fromValue + " cannot be converted to " + toValueType, e);
        }
    }

    @Nullable
    public static <T> T convertValue(@Nullable Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value: "
                    + fromValue + " cannot be converted to " + toValueTypeRef, e);
        }
    }

    @Nullable
    public static <T> T fromString(@Nullable String string, Class<T> clazz) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + string + " cannot be transformed to Json object", e);
        }
    }

    @Nullable
    public static <T> T fromString(@Nullable String string, TypeReference<T> valueTypeRef) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + string + " cannot be transformed to Json object", e);
        }
    }

    @Nullable
    public static <T> T fromBytes(@Nullable byte[] bytes, Class<T> clazz) {
        try {
            return bytes != null ? OBJECT_MAPPER.readValue(bytes, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + Arrays.toString(bytes) + " cannot be transformed to Json object", e);
        }
    }

    public static JsonNode fromBytes(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given byte[] value: "
                    + Arrays.toString(bytes) + " cannot be transformed to Json object", e);
        }
    }

    @Nullable
    public static String toString(@Nullable Object value) {
        try {
            return value != null ? OBJECT_MAPPER.writeValueAsString(value) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + value + " cannot be transformed to a String", e);
        }
    }

    public static String toPrettyString(Object o) {
        try {
            return PRETTY_SORTED_JSON_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T treeToValue(@NotNull JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't convert value: " + node, e);
        }
    }

    public static JsonNode toJsonNode(String value) {
        return toJsonNode(value, OBJECT_MAPPER);
    }

    public static JsonNode toJsonNode(@Nullable String value, @NotNull ObjectMapper mapper) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return mapper.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ObjectNode newObjectNode() {
        return newObjectNode(OBJECT_MAPPER);
    }

    public static ObjectNode newObjectNode(@NotNull ObjectMapper mapper) {
        return mapper.createObjectNode();
    }

    @Nullable
    public static <T> T clone(@NotNull T value) {
        @SuppressWarnings("unchecked")
        Class<T> valueClass = (Class<T>) value.getClass();
        return fromString(toString(value), valueClass);
    }

    public static <T> JsonNode valueToTree(T value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    public static <T> byte[] writeValueAsBytes(T value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + value + " cannot be transformed to a String", e);
        }
    }


    public static JsonNode getSafely(@Nullable JsonNode node, @NotNull String... path) {
        if (node == null) {
            return null;
        }
        for (String p : path) {
            if (!node.has(p)) {
                return null;
            } else {
                node = node.get(p);
            }
        }
        return node;
    }

    public static void replaceUuidsRecursively(@Nullable JsonNode node, @NotNull Set<String> skipFieldsSet, @NotNull UnaryOperator<UUID> replacer) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            @NotNull ObjectNode objectNode = (ObjectNode) node;
            @NotNull List<String> fieldNames = new ArrayList<>(objectNode.size());
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (skipFieldsSet.contains(fieldName)) {
                    continue;
                }
                var child = objectNode.get(fieldName);
                if (child.isObject() || child.isArray()) {
                    replaceUuidsRecursively(child, skipFieldsSet, replacer);
                } else if (child.isTextual()) {
                    String text = child.asText();
                    String newText = RegexUtils.replace(text, RegexUtils.UUID_PATTERN, uuid -> replacer.apply(UUID.fromString(uuid)).toString());
                    if (!text.equals(newText)) {
                        objectNode.put(fieldName, newText);
                    }
                }
            }
        } else if (node.isArray()) {
            @NotNull ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                JsonNode arrayElement = array.get(i);
                if (arrayElement.isObject() || arrayElement.isArray()) {
                    replaceUuidsRecursively(arrayElement, skipFieldsSet, replacer);
                } else if (arrayElement.isTextual()) {
                    String text = arrayElement.asText();
                    String newText = RegexUtils.replace(text, RegexUtils.UUID_PATTERN, uuid -> replacer.apply(UUID.fromString(uuid)).toString());
                    if (!text.equals(newText)) {
                        array.set(i, newText);
                    }
                }
            }
        }
    }

    public static void addKvEntry(@NotNull ObjectNode entityNode, @NotNull KvEntry kvEntry) {
        addKvEntry(entityNode, kvEntry, kvEntry.getKey());
    }

    public static void addKvEntry(@NotNull ObjectNode entityNode, @NotNull KvEntry kvEntry, String key) {
        addKvEntry(entityNode, kvEntry, key, OBJECT_MAPPER);
    }

    public static void addKvEntry(@NotNull ObjectNode entityNode, @NotNull KvEntry kvEntry, String key, @NotNull ObjectMapper mapper) {
        if (kvEntry.getDataType() == DataType.BOOLEAN) {
            kvEntry.getBooleanValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.DOUBLE) {
            kvEntry.getDoubleValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.LONG) {
            kvEntry.getLongValue().ifPresent(value -> entityNode.put(key, value));
        } else if (kvEntry.getDataType() == DataType.JSON) {
            if (kvEntry.getJsonValue().isPresent()) {
                entityNode.set(key, toJsonNode(kvEntry.getJsonValue().get(), mapper));
            }
        } else {
            entityNode.put(key, kvEntry.getValueAsString());
        }
    }

}
