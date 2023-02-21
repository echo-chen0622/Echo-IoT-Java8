package org.echoiot.server.common.transport.adaptor;

import com.google.gson.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonConverter {

    private static final Gson GSON = new Gson();
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";

    private static boolean isTypeCastEnabled = true;

    private static int maxStringValueLength = 0;

    public static PostTelemetryMsg convertToTelemetryProto(@NotNull JsonElement jsonElement, long ts) throws JsonSyntaxException {
        PostTelemetryMsg.Builder builder = PostTelemetryMsg.newBuilder();
        convertToTelemetry(jsonElement, ts, null, builder);
        return builder.build();
    }

    public static PostTelemetryMsg convertToTelemetryProto(@NotNull JsonElement jsonElement) throws JsonSyntaxException {
        return convertToTelemetryProto(jsonElement, System.currentTimeMillis());
    }

    private static void convertToTelemetry(@NotNull JsonElement jsonElement, long systemTs, Map<Long, List<KvEntry>> result, @NotNull PostTelemetryMsg.Builder builder) {
        if (jsonElement.isJsonObject()) {
            parseObject(systemTs, result, builder, jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            jsonElement.getAsJsonArray().forEach(je -> {
                if (je.isJsonObject()) {
                    parseObject(systemTs, result, builder, je.getAsJsonObject());
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + je);
                }
            });
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
    }

    private static void parseObject(long systemTs, @Nullable Map<Long, List<KvEntry>> result, @NotNull PostTelemetryMsg.Builder builder, @NotNull JsonObject jo) {
        if (result != null) {
            parseObject(result, systemTs, jo);
        } else {
            parseObject(builder, systemTs, jo);
        }
    }

    public static ClaimDeviceMsg convertToClaimDeviceProto(@NotNull DeviceId deviceId, @Nullable String json) {
        long durationMs = 0L;
        if (json != null && !json.isEmpty()) {
            return convertToClaimDeviceProto(deviceId, JSON_PARSER.parse(json));
        }
        return buildClaimDeviceMsg(deviceId, DataConstants.DEFAULT_SECRET_KEY, durationMs);
    }

    public static ClaimDeviceMsg convertToClaimDeviceProto(@NotNull DeviceId deviceId, @NotNull JsonElement jsonElement) {
        String secretKey = DataConstants.DEFAULT_SECRET_KEY;
        long durationMs = 0L;
        if (jsonElement.isJsonObject()) {
            JsonObject jo = jsonElement.getAsJsonObject();
            if (jo.has(DataConstants.SECRET_KEY_FIELD_NAME)) {
                secretKey = jo.get(DataConstants.SECRET_KEY_FIELD_NAME).getAsString();
            }
            if (jo.has(DataConstants.DURATION_MS_FIELD_NAME)) {
                durationMs = jo.get(DataConstants.DURATION_MS_FIELD_NAME).getAsLong();
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
        return buildClaimDeviceMsg(deviceId, secretKey, durationMs);
    }

    private static ClaimDeviceMsg buildClaimDeviceMsg(@NotNull DeviceId deviceId, String secretKey, long durationMs) {
        ClaimDeviceMsg.Builder result = ClaimDeviceMsg.newBuilder();
        return result
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setSecretKey(secretKey)
                .setDurationMs(durationMs)
                .build();
    }

    public static PostAttributeMsg convertToAttributesProto(@NotNull JsonElement jsonObject) throws JsonSyntaxException {
        if (jsonObject.isJsonObject()) {
            PostAttributeMsg.Builder result = PostAttributeMsg.newBuilder();
            @NotNull List<KeyValueProto> keyValueList = parseProtoValues(jsonObject.getAsJsonObject());
            result.addAllKv(keyValueList);
            return result.build();
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonObject);
        }
    }

    @NotNull
    public static JsonElement toJson(@NotNull TransportProtos.ToDeviceRpcRequestMsg msg, boolean includeRequestId) {
        @NotNull JsonObject result = new JsonObject();
        if (includeRequestId) {
            result.addProperty("id", msg.getRequestId());
        }
        result.addProperty("method", msg.getMethodName());
        result.add("params", JSON_PARSER.parse(msg.getParams()));
        return result;
    }

    private static void parseObject(@NotNull PostTelemetryMsg.Builder builder, long systemTs, @NotNull JsonObject jo) {
        if (jo.has("ts") && jo.has("values")) {
            parseWithTs(builder, jo);
        } else {
            parseWithoutTs(builder, systemTs, jo);
        }
    }

    private static void parseWithoutTs(@NotNull PostTelemetryMsg.Builder request, long systemTs, @NotNull JsonObject jo) {
        TsKvListProto.Builder builder = TsKvListProto.newBuilder();
        builder.setTs(systemTs);
        builder.addAllKv(parseProtoValues(jo));
        request.addTsKvList(builder.build());
    }

    private static void parseWithTs(@NotNull PostTelemetryMsg.Builder request, @NotNull JsonObject jo) {
        TsKvListProto.Builder builder = TsKvListProto.newBuilder();
        builder.setTs(jo.get("ts").getAsLong());
        builder.addAllKv(parseProtoValues(jo.get("values").getAsJsonObject()));
        request.addTsKvList(builder.build());
    }

    @NotNull
    private static List<KeyValueProto> parseProtoValues(@NotNull JsonObject valuesObject) {
        @NotNull List<KeyValueProto> result = new ArrayList<>();
        for (@NotNull Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    if (maxStringValueLength > 0 && value.getAsString().length() > maxStringValueLength) {
                        String message = String.format("String value length [%d] for key [%s] is greater than maximum allowed [%d]", value.getAsString().length(), valueEntry.getKey(), maxStringValueLength);
                        throw new JsonSyntaxException(message);
                    }
                    if (isTypeCastEnabled && NumberUtils.isParsable(value.getAsString())) {
                        try {
                            result.add(buildNumericKeyValueProto(value, valueEntry.getKey()));
                        } catch (RuntimeException th) {
                            result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.STRING_V)
                                    .setStringV(value.getAsString()).build());
                        }
                    } else {
                        result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.STRING_V)
                                .setStringV(value.getAsString()).build());
                    }
                } else if (value.isBoolean()) {
                    result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.BOOLEAN_V)
                            .setBoolV(value.getAsBoolean()).build());
                } else if (value.isNumber()) {
                    result.add(buildNumericKeyValueProto(value, valueEntry.getKey()));
                } else if (!value.isJsonNull()) {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else if (element.isJsonObject() || element.isJsonArray()) {
                result.add(KeyValueProto.newBuilder().setKey(valueEntry.getKey()).setType(KeyValueType.JSON_V).setJsonV(element.toString()).build());
            } else if (!element.isJsonNull()) {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
    }

    private static KeyValueProto buildNumericKeyValueProto(@NotNull JsonPrimitive value, String key) {
        String valueAsString = value.getAsString();
        KeyValueProto.Builder builder = KeyValueProto.newBuilder().setKey(key);
        @NotNull var bd = new BigDecimal(valueAsString);
        if (bd.stripTrailingZeros().scale() <= 0 && !isSimpleDouble(valueAsString)) {
            try {
                return builder.setType(KeyValueType.LONG_V).setLongV(bd.longValueExact()).build();
            } catch (ArithmeticException e) {
                if (isTypeCastEnabled) {
                    return builder.setType(KeyValueType.STRING_V).setStringV(bd.toPlainString()).build();
                } else {
                    throw new JsonSyntaxException("Big integer values are not supported!");
                }
            }
        } else {
            if (bd.scale() <= 16) {
                return builder.setType(KeyValueType.DOUBLE_V).setDoubleV(bd.doubleValue()).build();
            } else if (isTypeCastEnabled) {
                return builder.setType(KeyValueType.STRING_V).setStringV(bd.toPlainString()).build();
            } else {
                throw new JsonSyntaxException("Big integer values are not supported!");
            }
        }

    }

    private static boolean isSimpleDouble(@NotNull String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

    public static TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(@NotNull JsonElement json, int requestId) throws JsonSyntaxException {
        JsonObject object = json.getAsJsonObject();
        return TransportProtos.ToServerRpcRequestMsg.newBuilder().setRequestId(requestId).setMethodName(object.get("method").getAsString()).setParams(GSON.toJson(object.get("params"))).build();
    }

    private static void parseNumericValue(@NotNull List<KvEntry> result, @NotNull Entry<String, JsonElement> valueEntry, @NotNull JsonPrimitive value) {
        String valueAsString = value.getAsString();
        String key = valueEntry.getKey();
        @NotNull var bd = new BigDecimal(valueAsString);
        if (bd.stripTrailingZeros().scale() <= 0 && !isSimpleDouble(valueAsString)) {
            try {
                result.add(new LongDataEntry(key, bd.longValueExact()));
            } catch (ArithmeticException e) {
                if (isTypeCastEnabled) {
                    result.add(new StringDataEntry(key, bd.toPlainString()));
                } else {
                    throw new JsonSyntaxException("Big integer values are not supported!");
                }
            }
        } else {
            if (bd.scale() <= 16) {
                result.add(new DoubleDataEntry(key, bd.doubleValue()));
            } else if (isTypeCastEnabled) {
                result.add(new StringDataEntry(key, bd.toPlainString()));
            } else {
                throw new JsonSyntaxException("Big integer values are not supported!");
            }
        }
    }

    @NotNull
    public static JsonObject toJson(@NotNull GetAttributeResponseMsg payload) {
        @NotNull JsonObject result = new JsonObject();
        if (payload.getClientAttributeListCount() > 0) {
            @NotNull JsonObject attrObject = new JsonObject();
            payload.getClientAttributeListList().forEach(addToObjectFromProto(attrObject));
            result.add("client", attrObject);
        }
        if (payload.getSharedAttributeListCount() > 0) {
            @NotNull JsonObject attrObject = new JsonObject();
            payload.getSharedAttributeListList().forEach(addToObjectFromProto(attrObject));
            result.add("shared", attrObject);
        }
        return result;
    }

    @NotNull
    public static JsonObject toJson(@NotNull AttributeUpdateNotificationMsg payload) {
        @NotNull JsonObject result = new JsonObject();
        if (payload.getSharedUpdatedCount() > 0) {
            payload.getSharedUpdatedList().forEach(addToObjectFromProto(result));
        }
        if (payload.getSharedDeletedCount() > 0) {
            @NotNull JsonArray attrObject = new JsonArray();
            payload.getSharedDeletedList().forEach(attrObject::add);
            result.add("deleted", attrObject);
        }
        return result;
    }

    @NotNull
    public static JsonObject getJsonObjectForGateway(
            String deviceName,
            @NotNull TransportProtos.GetAttributeResponseMsg responseMsg
                                                    ) {
        @NotNull JsonObject result = new JsonObject();
        result.addProperty("id", responseMsg.getRequestId());
        result.addProperty(DEVICE_PROPERTY, deviceName);
        if (responseMsg.getClientAttributeListCount() > 0) {
            addValues(result, responseMsg.getClientAttributeListList(), responseMsg.getIsMultipleAttributesRequest());
        }
        if (responseMsg.getSharedAttributeListCount() > 0) {
            addValues(result, responseMsg.getSharedAttributeListList(), responseMsg.getIsMultipleAttributesRequest());
        }
        return result;
    }

    @NotNull
    public static JsonObject getJsonObjectForGateway(String deviceName, @NotNull AttributeUpdateNotificationMsg
            notificationMsg) {
        @NotNull JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", toJson(notificationMsg));
        return result;
    }

    private static void addValues(@NotNull JsonObject result, @NotNull List<TransportProtos.TsKvProto> kvList, boolean multipleAttrKeysRequested) {
        if (kvList.size() == 1 && !multipleAttrKeysRequested) {
            addValueToJson(result, "value", kvList.get(0).getKv());
        } else {
            JsonObject values;
            if (result.has("values")) {
                values = result.get("values").getAsJsonObject();
            } else {
                values = new JsonObject();
                result.add("values", values);
            }
            kvList.forEach(value -> addValueToJson(values, value.getKv().getKey(), value.getKv()));
        }
    }

    private static void addValueToJson(@NotNull JsonObject json, @NotNull String name, @NotNull TransportProtos.KeyValueProto entry) {
        switch (entry.getType()) {
            case BOOLEAN_V:
                json.addProperty(name, entry.getBoolV());
                break;
            case STRING_V:
                json.addProperty(name, entry.getStringV());
                break;
            case DOUBLE_V:
                json.addProperty(name, entry.getDoubleV());
                break;
            case LONG_V:
                json.addProperty(name, entry.getLongV());
                break;
            case JSON_V:
                json.add(name, JSON_PARSER.parse(entry.getJsonV()));
                break;
        }
    }

    @NotNull
    private static Consumer<TsKvProto> addToObjectFromProto(@NotNull JsonObject result) {
        return de -> {
            switch (de.getKv().getType()) {
                case BOOLEAN_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getBoolV()));
                    break;
                case DOUBLE_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getDoubleV()));
                    break;
                case LONG_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getLongV()));
                    break;
                case STRING_V:
                    result.add(de.getKv().getKey(), new JsonPrimitive(de.getKv().getStringV()));
                    break;
                case JSON_V:
                    result.add(de.getKv().getKey(), JSON_PARSER.parse(de.getKv().getJsonV()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getKv().getType());
            }
        };
    }

    @NotNull
    private static Consumer<AttributeKvEntry> addToObject(@NotNull JsonObject result) {
        return de -> {
            switch (de.getDataType()) {
                case BOOLEAN:
                    result.add(de.getKey(), new JsonPrimitive(de.getBooleanValue().get()));
                    break;
                case DOUBLE:
                    result.add(de.getKey(), new JsonPrimitive(de.getDoubleValue().get()));
                    break;
                case LONG:
                    result.add(de.getKey(), new JsonPrimitive(de.getLongValue().get()));
                    break;
                case STRING:
                    result.add(de.getKey(), new JsonPrimitive(de.getStrValue().get()));
                    break;
                case JSON:
                    result.add(de.getKey(), JSON_PARSER.parse(de.getJsonValue().get()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + de.getDataType());
            }
        };
    }

    public static JsonElement toJson(@NotNull TransportProtos.ToServerRpcResponseMsg msg) {
        if (StringUtils.isEmpty(msg.getError())) {
            return JSON_PARSER.parse(msg.getPayload());
        } else {
            @NotNull JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("error", msg.getError());
            return errorMsg;
        }
    }

    @NotNull
    public static JsonObject toJson(@NotNull ProvisionDeviceResponseMsg payload) {
        return toJson(payload, false, 0);
    }

    @NotNull
    public static JsonObject toJson(@NotNull ProvisionDeviceResponseMsg payload, int requestId) {
        return toJson(payload, true, requestId);
    }

    @NotNull
    private static JsonObject toJson(@NotNull ProvisionDeviceResponseMsg payload, boolean toGateway, int requestId) {
        @NotNull JsonObject result = new JsonObject();
        if (payload.getStatus() == ResponseStatus.NOT_FOUND) {
            result.addProperty("errorMsg", "Provision data was not found!");
            result.addProperty("status", ResponseStatus.NOT_FOUND.name());
        } else if (payload.getStatus() == TransportProtos.ResponseStatus.FAILURE) {
            result.addProperty("errorMsg", "Failed to provision device!");
            result.addProperty("status", ResponseStatus.FAILURE.name());
        } else {
            if (toGateway) {
                result.addProperty("id", requestId);
            }
            switch (payload.getCredentialsType()) {
                case ACCESS_TOKEN:
                case X509_CERTIFICATE:
                    result.addProperty("credentialsValue", payload.getCredentialsValue());
                    break;
                case MQTT_BASIC:
                    result.add("credentialsValue", JSON_PARSER.parse(payload.getCredentialsValue()).getAsJsonObject());
                    break;
                case LWM2M_CREDENTIALS:
                    break;
            }
            result.addProperty("credentialsType", payload.getCredentialsType().name());
            result.addProperty("status", ResponseStatus.SUCCESS.name());
        }
        return result;
    }

    @NotNull
    public static JsonElement toErrorJson(String errorMsg) {
        @NotNull JsonObject error = new JsonObject();
        error.addProperty("error", errorMsg);
        return error;
    }

    @NotNull
    public static JsonElement toGatewayJson(String deviceName, @NotNull TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        @NotNull JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", JsonConverter.toJson(rpcRequest, true));
        return result;
    }

    @NotNull
    public static JsonElement toGatewayJson(String deviceName, @NotNull TransportProtos.ProvisionDeviceResponseMsg
            responseRequest) {
        @NotNull JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, deviceName);
        result.add("data", JsonConverter.toJson(responseRequest));
        return result;
    }

    @NotNull
    public static Set<AttributeKvEntry> convertToAttributes(@NotNull JsonElement element) {
        @NotNull Set<AttributeKvEntry> result = new HashSet<>();
        long ts = System.currentTimeMillis();
        result.addAll(parseValues(element.getAsJsonObject()).stream().map(kv -> new BaseAttributeKvEntry(kv, ts)).collect(Collectors.toList()));
        return result;
    }

    @NotNull
    private static List<KvEntry> parseValues(@NotNull JsonObject valuesObject) {
        @NotNull List<KvEntry> result = new ArrayList<>();
        for (@NotNull Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
            JsonElement element = valueEntry.getValue();
            if (element.isJsonPrimitive()) {
                JsonPrimitive value = element.getAsJsonPrimitive();
                if (value.isString()) {
                    if (maxStringValueLength > 0 && value.getAsString().length() > maxStringValueLength) {
                        String message = String.format("String value length [%d] for key [%s] is greater than maximum allowed [%d]", value.getAsString().length(), valueEntry.getKey(), maxStringValueLength);
                        throw new JsonSyntaxException(message);
                    }
                    if (isTypeCastEnabled && NumberUtils.isParsable(value.getAsString())) {
                        try {
                            parseNumericValue(result, valueEntry, value);
                        } catch (RuntimeException th) {
                            result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
                        }
                    } else {
                        result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
                    }
                } else if (value.isBoolean()) {
                    result.add(new BooleanDataEntry(valueEntry.getKey(), value.getAsBoolean()));
                } else if (value.isNumber()) {
                    parseNumericValue(result, valueEntry, value);
                } else {
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + value);
                }
            } else if (element.isJsonObject() || element.isJsonArray()) {
                result.add(new JsonDataEntry(valueEntry.getKey(), element.toString()));
            } else {
                throw new JsonSyntaxException(CAN_T_PARSE_VALUE + element);
            }
        }
        return result;
    }

    @NotNull
    public static Map<Long, List<KvEntry>> convertToTelemetry(@NotNull JsonElement jsonElement, long systemTs) throws
            JsonSyntaxException {
        return convertToTelemetry(jsonElement, systemTs, false);
    }

    @NotNull
    public static Map<Long, List<KvEntry>> convertToSortedTelemetry(@NotNull JsonElement jsonElement, long systemTs) throws
            JsonSyntaxException {
        return convertToTelemetry(jsonElement, systemTs, true);
    }

    @NotNull
    public static Map<Long, List<KvEntry>> convertToTelemetry(@NotNull JsonElement jsonElement, long systemTs, boolean sorted) throws
            JsonSyntaxException {
        @NotNull Map<Long, List<KvEntry>> result = sorted ? new TreeMap<>() : new HashMap<>();
        convertToTelemetry(jsonElement, systemTs, result, null);
        return result;
    }


    private static void parseObject(@NotNull Map<Long, List<KvEntry>> result, long systemTs, @NotNull JsonObject jo) {
        if (jo.has("ts") && jo.has("values")) {
            parseWithTs(result, jo);
        } else {
            parseWithoutTs(result, systemTs, jo);
        }
    }

    private static void parseWithoutTs(@NotNull Map<Long, List<KvEntry>> result, long systemTs, @NotNull JsonObject jo) {
        for (KvEntry entry : parseValues(jo)) {
            result.computeIfAbsent(systemTs, tmp -> new ArrayList<>()).add(entry);
        }
    }

    public static void parseWithTs(@NotNull Map<Long, List<KvEntry>> result, @NotNull JsonObject jo) {
        long ts = jo.get("ts").getAsLong();
        JsonObject valuesObject = jo.get("values").getAsJsonObject();
        for (KvEntry entry : parseValues(valuesObject)) {
            result.computeIfAbsent(ts, tmp -> new ArrayList<>()).add(entry);
        }
    }

    public static JsonElement parse(@NotNull String json) {
        return JSON_PARSER.parse(json);
    }

    public static <T> T parse(@NotNull String json, Class<T> clazz) {
        return fromJson(parse(json), clazz);
    }

    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }

    public static JsonObject toJsonObject(Object o) {
        return (JsonObject) GSON.toJsonTree(o);
    }

    public static <T> T fromJson(JsonElement element, Class<T> type) {
        return GSON.fromJson(element, type);
    }

    public static void setTypeCastEnabled(boolean enabled) {
        isTypeCastEnabled = enabled;
    }

    public static void setMaxStringValueLength(int length) {
        maxStringValueLength = length;
    }

    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(@NotNull String json) {
        JsonElement jsonElement = JSON_PARSER.parse(json);
        if (jsonElement.isJsonObject()) {
            return buildProvisionRequestMsg(jsonElement.getAsJsonObject());
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + jsonElement);
        }
    }

    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(@NotNull JsonObject jo) {
        return buildProvisionRequestMsg(jo);
    }

    private static TransportProtos.ProvisionDeviceRequestMsg buildProvisionRequestMsg(@NotNull JsonObject jo) {
        return TransportProtos.ProvisionDeviceRequestMsg.newBuilder()
                .setDeviceName(getStrValue(jo, DataConstants.DEVICE_NAME, false))
                .setCredentialsType(jo.get(DataConstants.CREDENTIALS_TYPE) != null ? TransportProtos.CredentialsType.valueOf(getStrValue(jo, DataConstants.CREDENTIALS_TYPE, false)) : CredentialsType.ACCESS_TOKEN)
                .setCredentialsDataProto(TransportProtos.CredentialsDataProto.newBuilder()
                        .setValidateDeviceTokenRequestMsg(ValidateDeviceTokenRequestMsg.newBuilder().setToken(getStrValue(jo, DataConstants.TOKEN, false)).build())
                        .setValidateBasicMqttCredRequestMsg(ValidateBasicMqttCredRequestMsg.newBuilder()
                                .setClientId(getStrValue(jo, DataConstants.CLIENT_ID, false))
                                .setUserName(getStrValue(jo, DataConstants.USERNAME, false))
                                .setPassword(getStrValue(jo, DataConstants.PASSWORD, false))
                                .build())
                        .setValidateDeviceX509CertRequestMsg(ValidateDeviceX509CertRequestMsg.newBuilder()
                                .setHash(getStrValue(jo, DataConstants.HASH, false)).build())
                        .build())
                .setProvisionDeviceCredentialsMsg(buildProvisionDeviceCredentialsMsg(
                        getStrValue(jo, DataConstants.PROVISION_KEY, true),
                        getStrValue(jo, DataConstants.PROVISION_SECRET, true)))
                .build();
    }

    private static TransportProtos.ProvisionDeviceCredentialsMsg buildProvisionDeviceCredentialsMsg(String provisionKey, String provisionSecret) {
        return TransportProtos.ProvisionDeviceCredentialsMsg.newBuilder()
                .setProvisionDeviceKey(provisionKey)
                .setProvisionDeviceSecret(provisionSecret)
                .build();
    }


    private static String getStrValue(@NotNull JsonObject jo, String field, boolean requiredField) {
        if (jo.has(field)) {
            return jo.get(field).getAsString();
        } else {
            if (requiredField) {
                throw new RuntimeException("Failed to find the field " + field + " in JSON body " + jo + "!");
            }
            return "";
        }
    }
}
