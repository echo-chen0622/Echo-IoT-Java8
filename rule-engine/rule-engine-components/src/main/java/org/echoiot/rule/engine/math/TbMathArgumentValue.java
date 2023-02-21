package org.echoiot.rule.engine.math;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TbMathArgumentValue {

    @Getter
    private final double value;

    private TbMathArgumentValue(double value) {
        this.value = value;
    }

    @NotNull
    public static TbMathArgumentValue constant(@NotNull TbMathArgument arg) {
        return fromString(arg.getKey());
    }

    @NotNull
    private static TbMathArgumentValue defaultOrThrow(@Nullable Double defaultValue, String error) {
        if (defaultValue != null) {
            return new TbMathArgumentValue(defaultValue);
        }
        throw new RuntimeException(error);
    }

    @NotNull
    public static TbMathArgumentValue fromMessageBody(@NotNull TbMathArgument arg, @NotNull Optional<ObjectNode> jsonNodeOpt) {
        String key = arg.getKey();
        Double defaultValue = arg.getDefaultValue();
        if (jsonNodeOpt.isEmpty()) {
            return defaultOrThrow(defaultValue, "Message body is empty!");
        }
        @NotNull var json = jsonNodeOpt.get();
        if (!json.has(key)) {
            return defaultOrThrow(defaultValue, "Message body has no '" + key + "'!");
        }
        JsonNode valueNode = json.get(key);
        if (valueNode.isNull()) {
            return defaultOrThrow(defaultValue, "Message body has null '" + key + "'!");
        }
        double value;
        if (valueNode.isNumber()) {
            value = valueNode.doubleValue();
        } else if (valueNode.isTextual()) {
            var valueNodeText = valueNode.asText();
            if (StringUtils.isNotBlank(valueNodeText)) {
                try {
                    value = Double.parseDouble(valueNode.asText());
                } catch (NumberFormatException ne) {
                    throw new RuntimeException("Can't convert value '" + valueNode.asText() + "' to double!");
                }
            } else {
                return defaultOrThrow(defaultValue, "Message value is empty for '" + key + "'!");
            }
        } else {
            throw new RuntimeException("Can't convert value '" + valueNode + "' to double!");
        }
        return new TbMathArgumentValue(value);
    }

    @NotNull
    public static TbMathArgumentValue fromMessageMetadata(@NotNull TbMathArgument arg, @Nullable TbMsgMetaData metaData) {
        String key = arg.getKey();
        Double defaultValue = arg.getDefaultValue();
        if (metaData == null) {
            return defaultOrThrow(defaultValue, "Message metadata is empty!");
        }
        var value = metaData.getValue(key);
        if (StringUtils.isEmpty(value)) {
            return defaultOrThrow(defaultValue, "Message metadata has no '" + key + "'!");
        }
        return fromString(value);
    }

    @NotNull
    public static TbMathArgumentValue fromLong(long value) {
        return new TbMathArgumentValue(value);
    }

    @NotNull
    public static TbMathArgumentValue fromDouble(double value) {
        return new TbMathArgumentValue(value);
    }

    @NotNull
    public static TbMathArgumentValue fromString(@NotNull String value) {
        try {
            return new TbMathArgumentValue(Double.parseDouble(value));
        } catch (NumberFormatException ne) {
            throw new RuntimeException("Can't convert value '" + value + "' to double!");
        }
    }
}
