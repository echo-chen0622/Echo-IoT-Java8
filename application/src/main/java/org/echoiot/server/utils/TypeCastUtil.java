package org.echoiot.server.utils;

import org.apache.commons.lang3.math.NumberUtils;
import org.echoiot.server.common.data.kv.DataType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public class TypeCastUtil {

    private TypeCastUtil() {}

    @NotNull
    public static Map.Entry<DataType, Object> castValue(@NotNull String value) {
        if (isNumber(value)) {
            @NotNull String formattedValue = value.replace(',', '.');
            try {
                @NotNull BigDecimal bd = new BigDecimal(formattedValue);
                if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                    if (bd.scale() <= 16) {
                        return Map.entry(DataType.DOUBLE, bd.doubleValue());
                    }
                } else {
                    return Map.entry(DataType.LONG, bd.longValueExact());
                }
            } catch (RuntimeException ignored) {}
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Map.entry(DataType.BOOLEAN, Boolean.parseBoolean(value));
        }
        return Map.entry(DataType.STRING, value);
    }

    private static boolean isNumber(@NotNull String value) {
        return NumberUtils.isNumber(value.replace(',', '.'));
    }

    private static boolean isSimpleDouble(@NotNull String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

}
